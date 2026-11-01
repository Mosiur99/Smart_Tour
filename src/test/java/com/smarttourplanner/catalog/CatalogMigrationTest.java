package com.smarttourplanner.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttourplanner.support.TestDatabase;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CatalogMigrationTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static Flyway flyway;
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        DataSource dataSource = TestDatabase.createEmpty();
        flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void importsEveryDivisionAndDistrict() {
        assertThat(count("division")).isEqualTo(8);
        assertThat(count("district")).isEqualTo(64);
        assertThat(count("destination")).isEqualTo(64);
        assertThat(count("hub")).isEqualTo(64);
    }

    @Test
    void importsEverySourceRecordOnce() throws IOException {
        assertThat(countKind("SPOT")).isEqualTo(distinctNames("spots"));
        assertThat(countKind("HERITAGE")).isEqualTo(distinctNames("historicalPlaces"));
        assertThat(countKind("FOOD")).isEqualTo(distinctNames("foods"));
        assertThat(count("hotel")).isEqualTo(distinctNames("hotels"));
    }

    @Test
    void importsTransportBothWaysExceptDhakaToItself() {
        assertThat(count("transport_leg")).isEqualTo(206);
        Integer hubLoops = jdbc.queryForObject(
                "SELECT count(*) FROM transport_leg WHERE from_hub_id = to_hub_id", Integer.class);
        assertThat(hubLoops).isZero();
    }

    @Test
    void marksImportedRecordsAsNotYetPublished() {
        for (String table : new String[] {"destination", "poi", "hotel", "transport_leg"}) {
            Integer published = jdbc.queryForObject(
                    "SELECT count(*) FROM " + table + " WHERE status <> 'IMPORTED'", Integer.class);
            assertThat(published).as(table).isZero();
        }
    }

    @Test
    void readsTheFareRangeFromTheEstimateNote() {
        Integer fareMax = jdbc.queryForObject("""
                SELECT fare_max FROM transport_leg leg
                JOIN hub destination ON destination.id = leg.to_hub_id
                WHERE destination.slug = 'rajshahi' AND leg.mode = 'BUS'
                """, Integer.class);

        assertThat(fareMax).isEqualTo(930);
    }

    @Test
    void keepsUnknownPricesAsNullNeverZero() {
        Integer unpriced = jdbc.queryForObject("SELECT count(*) FROM hotel WHERE price_min IS NULL", Integer.class);
        Integer zeroPriced = jdbc.queryForObject("SELECT count(*) FROM hotel WHERE price_min = 0", Integer.class);

        assertThat(unpriced).isPositive();
        assertThat(zeroPriced).isZero();
    }

    @Test
    void storesBanglaNames() {
        String name = jdbc.queryForObject("SELECT name_bn FROM destination WHERE slug = 'rajshahi'", String.class);

        assertThat(name).isEqualTo("রাজশাহী");
    }

    @Test
    void runningMigrationsAgainChangesNothing() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void rejectsPublishingADestinationWithoutCoordinates() {
        assertRejected("UPDATE destination SET status = 'PUBLISHED' WHERE slug = 'rajshahi'");
    }

    @Test
    void rejectsPublishingASpotWithoutACheckedDate() {
        assertRejected("""
                UPDATE poi SET status = 'PUBLISHED', name_bn = 'স্থান', lat = 24.4, lng = 88.6, visit_minutes = 60
                WHERE id = (SELECT min(id) FROM poi WHERE kind = 'SPOT')
                """);
    }

    @Test
    void rejectsPublishingASpotWithoutCoordinates() {
        assertRejected("""
                UPDATE poi SET status = 'PUBLISHED', name_bn = 'স্থান', checked_at = current_date
                WHERE id = (SELECT min(id) FROM poi WHERE kind = 'SPOT')
                """);
    }

    @Test
    void rejectsPublishingAHotelWithoutAPrice() {
        assertRejected("""
                UPDATE hotel SET status = 'PUBLISHED', tier = 'MID', lat = 24.4, lng = 88.6,
                    source_url = 'https://example.com', checked_at = current_date
                WHERE id = (SELECT min(id) FROM hotel WHERE price_min IS NULL)
                """);
    }

    @Test
    void rejectsAFareRangeThatRunsBackwards() {
        assertRejected("UPDATE transport_leg SET fare_min = 900, fare_max = 100 WHERE id = (SELECT min(id) FROM transport_leg)");
    }

    @Test
    void rejectsAZeroPrice() {
        assertRejected("UPDATE hotel SET price_min = 0, price_max = 0 WHERE id = (SELECT min(id) FROM hotel)");
    }

    @Test
    void rejectsCoordinatesOutsideBangladesh() {
        assertRejected("UPDATE destination SET lat = 51.5 WHERE slug = 'rajshahi'");
    }

    @Test
    void rejectsADuplicatePlanForTheSameRequestAndVersion() {
        String insert = """
                INSERT INTO trip_plan (public_id, request_hash, catalog_version, request, result)
                VALUES (?, 'same-hash', 1, '{}', '{}')
                """;
        jdbc.update(insert, "plan-one");

        assertThatThrownBy(() -> jdbc.update(insert, "plan-two"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertRejected(String sql) {
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private int countKind(String kind) {
        return jdbc.queryForObject("SELECT count(*) FROM poi WHERE kind = ?::poi_kind", Integer.class, kind);
    }

    private int distinctNames(String field) throws IOException {
        int total = 0;
        for (JsonNode summary : read("data/districts-index.json")) {
            Set<String> names = new HashSet<>();
            JsonNode district = read("data/districts/" + summary.get("id").asString() + ".json");
            district.path(field).forEach(item -> names.add(item.get("name").asString()));
            total += names.size();
        }
        return total;
    }

    private static JsonNode read(String path) throws IOException {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return MAPPER.readTree(input);
        }
    }
}
