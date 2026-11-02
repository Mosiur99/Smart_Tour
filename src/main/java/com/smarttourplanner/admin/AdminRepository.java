package com.smarttourplanner.admin;

import com.smarttourplanner.admin.AdminModels.AuditEntry;
import com.smarttourplanner.admin.AdminModels.DestinationOverview;
import com.smarttourplanner.admin.AdminModels.RecordView;
import com.smarttourplanner.admin.AdminModels.StaleRecord;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Reads and writes any editable record. Table and column names come only from {@link Editable}. */
@Repository
class AdminRepository {

    private static final int HISTORY_LIMIT = 100;
    private static final int STALE_LIMIT = 200;
    private static final TypeReference<Map<String, @Nullable Object>> SNAPSHOT = new TypeReference<>() {
    };

    private final JdbcClient jdbc;
    private final JsonMapper json;

    AdminRepository(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    Optional<RecordView> find(Editable kind, long id) {
        return findWhere(kind, "t.id = :id", Map.of("id", id)).stream().findFirst();
    }

    Optional<RecordView> findDestination(String slug) {
        return findWhere(Editable.DESTINATION, "t.slug = :slug", Map.of("slug", slug)).stream().findFirst();
    }

    List<RecordView> findOwnedBy(Editable kind, long destinationId) {
        return findWhere(kind, "t.destination_id = :owner", Map.of("owner", destinationId));
    }

    List<RecordView> findLegsAtArrivalHubOf(long destinationId) {
        return findWhere(Editable.LEG, """
                (SELECT arrival_hub_id FROM destination WHERE id = :owner) IN (t.from_hub_id, t.to_hub_id)
                """, Map.of("owner", destinationId));
    }

    private List<RecordView> findWhere(Editable kind, String condition, Map<String, ?> params) {
        String columns = kind.fields.stream().map(Field::selectSql).collect(Collectors.joining(", "));
        String sql = """
                SELECT t.id, t.revision, t.status::text AS status, %s AS label, %s AS destination_slug, %s
                FROM %s t
                WHERE %s
                ORDER BY label, t.id
                """.formatted(kind.labelSql, kind.destinationSql, columns, kind.table, condition);
        return jdbc.sql(sql).params(params).query((rs, row) -> record(kind, rs)).list();
    }

    long insert(Editable kind, Map<Field, @Nullable Object> values, @Nullable Long destinationId) {
        List<String> columns = new ArrayList<>();
        List<String> expressions = new ArrayList<>();
        Map<String, @Nullable Object> params = new LinkedHashMap<>();
        values.forEach((field, value) -> {
            String parameter = "v" + params.size();
            columns.add(field.column());
            expressions.add(field.valueSql(parameter));
            params.put(parameter, sqlValue(value));
        });
        if (destinationId != null) {
            columns.add("destination_id");
            expressions.add(":destinationId");
            params.put("destinationId", destinationId);
        }
        String sql = "INSERT INTO %s (%s) VALUES (%s) RETURNING id"
                .formatted(kind.table, String.join(", ", columns), String.join(", ", expressions));
        return withParams(jdbc.sql(sql), params).query(Long.class).single();
    }

    /** Returns false when the revision no longer matches, because someone else saved first. */
    boolean update(Editable kind, long id, int revision, Map<Field, @Nullable Object> values) {
        List<String> assignments = new ArrayList<>();
        Map<String, @Nullable Object> params = new LinkedHashMap<>();
        values.forEach((field, value) -> {
            String parameter = "v" + params.size();
            assignments.add(field.column() + " = " + field.valueSql(parameter));
            params.put(parameter, sqlValue(value));
        });
        params.put("id", id);
        params.put("revision", revision);
        String sql = "UPDATE %s t SET %s, revision = revision + 1 WHERE id = :id AND revision = :revision"
                .formatted(kind.table, String.join(", ", assignments));
        return withParams(jdbc.sql(sql), params).update() == 1;
    }

    /** Returns false when the revision no longer matches, because someone else saved first. */
    boolean updateStatus(Editable kind, long id, int revision, String status) {
        return jdbc.sql("""
                UPDATE %s SET status = CAST(:status AS record_status), revision = revision + 1
                WHERE id = :id AND revision = :revision
                """.formatted(kind.table))
                .param("status", status)
                .param("id", id)
                .param("revision", revision)
                .update() == 1;
    }

    boolean hubExists(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM hub WHERE slug = :slug)")
                .param("slug", slug)
                .query(Boolean.class)
                .single();
    }

    Optional<Long> destinationId(String slug) {
        return jdbc.sql("SELECT id FROM destination WHERE slug = :slug").param("slug", slug).query(Long.class).optional();
    }

    /** Old cache entries stop matching once the version goes up. */
    void raiseCatalogVersion() {
        jdbc.sql("UPDATE catalog_version SET version = version + 1").update();
    }

    void audit(String actor, Editable kind, long id, String action,
            @Nullable RecordView before, @Nullable RecordView after) {
        jdbc.sql("""
                INSERT INTO audit_log (actor, entity, entity_id, action, before, after)
                VALUES (:actor, :entity, :id, :action, CAST(:before AS jsonb), CAST(:after AS jsonb))
                """)
                .param("actor", actor)
                .param("entity", kind.path)
                .param("id", id)
                .param("action", action)
                .param("before", snapshot(before))
                .param("after", snapshot(after))
                .update();
    }

    List<AuditEntry> history(@Nullable Editable kind, @Nullable Long id) {
        return jdbc.sql("""
                SELECT id, actor, entity, entity_id, action, before::text AS before, after::text AS after, created_at
                FROM audit_log
                WHERE (:entity::text IS NULL OR entity = :entity) AND (:entityId::bigint IS NULL OR entity_id = :entityId)
                ORDER BY created_at DESC, id DESC
                LIMIT :limit
                """)
                .param("entity", kind == null ? null : kind.path)
                .param("entityId", id)
                .param("limit", HISTORY_LIMIT)
                .query((rs, row) -> new AuditEntry(
                        rs.getLong("id"), rs.getString("actor"), rs.getString("entity"), rs.getLong("entity_id"),
                        rs.getString("action"), parse(rs.getString("before")), parse(rs.getString("after")),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    List<DestinationOverview> destinations(int staleAfterDays) {
        return jdbc.sql("""
                WITH item AS (
                  SELECT destination_id, status, checked_at FROM poi
                  UNION ALL
                  SELECT destination_id, status, checked_at FROM hotel
                )
                SELECT d.slug, d.name_en, d.name_bn, d.status::text AS status,
                       count(item.destination_id) AS records,
                       count(*) FILTER (WHERE item.status = 'PUBLISHED') AS published,
                       count(*) FILTER (WHERE item.status = 'PUBLISHED' AND item.checked_at < current_date - :days)
                         AS stale
                FROM destination d
                LEFT JOIN item ON item.destination_id = d.id
                GROUP BY d.id
                ORDER BY d.name_en
                """)
                .param("days", staleAfterDays)
                .query((rs, row) -> new DestinationOverview(
                        rs.getString("slug"), rs.getString("name_en"), rs.getString("name_bn"),
                        rs.getString("status"), rs.getInt("records"), rs.getInt("published"), rs.getInt("stale")))
                .list();
    }

    /** Published records checked longest ago come first. */
    List<StaleRecord> stale(int staleAfterDays) {
        String union = Stream.of(Editable.PLACE, Editable.HOTEL, Editable.LEG)
                .map(kind -> """
                        SELECT '%s' AS entity, t.id, %s AS label, %s AS destination_slug, t.checked_at
                        FROM %s t
                        WHERE t.status = 'PUBLISHED' AND t.checked_at < current_date - :days
                        """.formatted(kind.path, kind.labelSql, kind.destinationSql, kind.table))
                .collect(Collectors.joining("UNION ALL\n"));
        return jdbc.sql(union + "ORDER BY checked_at, label LIMIT :limit")
                .param("days", staleAfterDays)
                .param("limit", STALE_LIMIT)
                .query((rs, row) -> new StaleRecord(
                        rs.getString("entity"), rs.getLong("id"), rs.getString("label"),
                        rs.getString("destination_slug"), rs.getDate("checked_at").toLocalDate()))
                .list();
    }

    private RecordView record(Editable kind, ResultSet rs) throws SQLException {
        Map<String, @Nullable Object> values = new LinkedHashMap<>();
        for (Field field : kind.fields) {
            values.put(field.name(), plain(rs.getObject(field.name())));
        }
        return new RecordView(kind.path, rs.getLong("id"), rs.getInt("revision"), rs.getString("status"),
                rs.getString("label"), rs.getString("destination_slug"), values);
    }

    /** Turns database types into ones that read naturally as JSON. */
    private static @Nullable Object plain(@Nullable Object value) throws SQLException {
        return switch (value) {
            case null -> null;
            case Array array -> Arrays.stream((Object[]) array.getArray()).map(AdminRepository::listItem).toList();
            case Date date -> date.toLocalDate();
            case Short number -> number.intValue();
            // 24.89000 reads as 24.89, and 20.00 as 20 rather than 2E+1.
            case BigDecimal number -> new BigDecimal(number.stripTrailingZeros().toPlainString());
            default -> value;
        };
    }

    /** List columns hold small numbers or text. */
    private static Object listItem(Object item) {
        return item instanceof Short number ? number.intValue() : item;
    }

    private @Nullable Object sqlValue(@Nullable Object value) {
        return value instanceof List<?> list ? json.writeValueAsString(list) : value;
    }

    private @Nullable String snapshot(@Nullable RecordView record) {
        if (record == null) {
            return null;
        }
        Map<String, @Nullable Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", record.status());
        snapshot.putAll(record.values());
        return json.writeValueAsString(snapshot);
    }

    private @Nullable Map<String, @Nullable Object> parse(@Nullable String text) {
        return text == null ? null : json.readValue(text, SNAPSHOT);
    }

    private static StatementSpec withParams(StatementSpec statement, Map<String, @Nullable Object> params) {
        params.forEach(statement::param);
        return statement;
    }
}
