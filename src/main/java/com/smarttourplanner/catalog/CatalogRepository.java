package com.smarttourplanner.catalog;

import com.smarttourplanner.catalog.CatalogModels.DestinationSummary;
import com.smarttourplanner.catalog.CatalogModels.Hotel;
import com.smarttourplanner.catalog.CatalogModels.Hub;
import com.smarttourplanner.catalog.CatalogModels.Place;
import com.smarttourplanner.catalog.CatalogModels.TransportOption;
import com.smarttourplanner.catalog.CatalogModels.University;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {

    private static final String VISIBLE = "status IN ('IMPORTED', 'PUBLISHED')";

    private static final String SUMMARY_SELECT = """
            SELECT d.id, d.slug, d.name_en, d.name_bn, d.status,
                   v.slug AS division_slug, v.name_en AS division_en, v.name_bn AS division_bn
            FROM destination d
            JOIN district t ON t.id = d.district_id
            JOIN division v ON v.id = t.division_id
            """;

    private final JdbcClient jdbc;

    public CatalogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public int currentVersion() {
        return jdbc.sql("SELECT version FROM catalog_version").query(Integer.class).single();
    }

    public List<Hub> findHubs() {
        return jdbc.sql("SELECT slug, name_en, name_bn FROM hub ORDER BY name_en")
                .query((rs, row) -> new Hub(rs.getString("slug"), rs.getString("name_en"), rs.getString("name_bn")))
                .list();
    }

    /** Returns one row more than the limit, so the caller can tell whether another page exists. */
    public List<DestinationSummary> findDestinations(String division, String afterSlug, int limit) {
        return jdbc.sql(SUMMARY_SELECT + """
                WHERE d.status IN ('IMPORTED', 'PUBLISHED')
                  AND (:division::text IS NULL OR v.slug = :division)
                  AND (:after::text IS NULL OR d.slug > :after)
                ORDER BY d.slug
                LIMIT :limit
                """)
                .param("division", division)
                .param("after", afterSlug)
                .param("limit", limit + 1)
                .query(CatalogRepository::summary)
                .list();
    }

    /** Matches part of a name, tolerates spelling mistakes, and puts the closest names first. */
    public List<DestinationSummary> searchDestinations(String query, String division, int limit) {
        return jdbc.sql(SUMMARY_SELECT + """
                WHERE d.status IN ('IMPORTED', 'PUBLISHED')
                  AND (:division::text IS NULL OR v.slug = :division)
                  AND (d.name_en ILIKE :contains ESCAPE '\\'
                       OR d.name_bn ILIKE :contains ESCAPE '\\'
                       OR word_similarity(:query, d.name_en) > 0.4
                       OR word_similarity(:query, d.name_bn) > 0.3)
                ORDER BY greatest(word_similarity(:query, d.name_en), word_similarity(:query, d.name_bn)) DESC, d.slug
                LIMIT :limit
                """)
                .param("division", division)
                .param("contains", "%" + escapeLike(query) + "%")
                .param("query", query)
                .param("limit", limit)
                .query(CatalogRepository::summary)
                .list();
    }

    public Optional<DestinationRow> findDestination(String slug) {
        return jdbc.sql("""
                SELECT d.slug, d.name_en, d.name_bn, d.status, d.id, d.arrival_hub_id, d.lat, d.lng,
                       d.local_transport_daily, d.food_daily, d.travel_note, d.hotel_note, d.university_note, d.guide_url,
                       v.slug AS division_slug, v.name_en AS division_en, v.name_bn AS division_bn
                FROM destination d
                JOIN district t ON t.id = d.district_id
                JOIN division v ON v.id = t.division_id
                WHERE d.slug = :slug AND d.status IN ('IMPORTED', 'PUBLISHED')
                """)
                .param("slug", slug)
                .query((rs, row) -> new DestinationRow(
                        rs.getLong("id"),
                        rs.getObject("arrival_hub_id", Long.class),
                        summary(rs, row),
                        toDouble(rs.getBigDecimal("lat")),
                        toDouble(rs.getBigDecimal("lng")),
                        rs.getObject("local_transport_daily", Integer.class),
                        rs.getObject("food_daily", Integer.class),
                        rs.getString("travel_note"),
                        rs.getString("hotel_note"),
                        rs.getString("university_note"),
                        rs.getString("guide_url")))
                .optional();
    }

    public List<Place> findPlaces(long destinationId, String kind) {
        return jdbc.sql("""
                SELECT name_en, name_bn, lat, lng, priority, visit_minutes, fee_min, fee_max, tags, note, checked_at, status
                FROM poi
                WHERE destination_id = :destination AND kind = :kind::poi_kind AND %s
                ORDER BY priority, name_en
                """.formatted(VISIBLE))
                .param("destination", destinationId)
                .param("kind", kind)
                .query((rs, row) -> new Place(
                        rs.getString("name_en"),
                        rs.getString("name_bn"),
                        toDouble(rs.getBigDecimal("lat")),
                        toDouble(rs.getBigDecimal("lng")),
                        rs.getInt("priority"),
                        rs.getObject("visit_minutes", Integer.class),
                        rs.getObject("fee_min", Integer.class),
                        rs.getObject("fee_max", Integer.class),
                        toList(rs.getArray("tags")),
                        rs.getString("note"),
                        toLocalDate(rs.getDate("checked_at")),
                        isVerified(rs)))
                .list();
    }

    public List<Hotel> findHotels(long destinationId) {
        return jdbc.sql("""
                SELECT name_en, name_bn, tier, address, room_capacity, price_min, price_max, website, note,
                       checked_at, status
                FROM hotel
                WHERE destination_id = :destination AND %s
                ORDER BY name_en
                """.formatted(VISIBLE))
                .param("destination", destinationId)
                .query((rs, row) -> new Hotel(
                        rs.getString("name_en"),
                        rs.getString("name_bn"),
                        rs.getString("tier"),
                        rs.getString("address"),
                        rs.getObject("room_capacity", Integer.class),
                        rs.getObject("price_min", Integer.class),
                        rs.getObject("price_max", Integer.class),
                        rs.getString("website"),
                        rs.getString("note"),
                        toLocalDate(rs.getDate("checked_at")),
                        isVerified(rs)))
                .list();
    }

    public List<University> findUniversities(long destinationId) {
        return jdbc.sql("""
                SELECT name_en, kind, location, note FROM university
                WHERE destination_id = :destination
                ORDER BY name_en
                """)
                .param("destination", destinationId)
                .query((rs, row) -> new University(
                        rs.getString("name_en"), rs.getString("kind"), rs.getString("location"), rs.getString("note")))
                .list();
    }

    public List<TransportOption> findLegsArrivingAt(long hubId) {
        return findLegs("leg.to_hub_id = :hub", hubId);
    }

    public List<TransportOption> findLegsLeaving(long hubId) {
        return findLegs("leg.from_hub_id = :hub", hubId);
    }

    private List<TransportOption> findLegs(String hubFilter, long hubId) {
        return jdbc.sql("""
                SELECT source.slug AS from_hub, target.slug AS to_hub, leg.mode, leg.service_name,
                       leg.duration_min, leg.departure_note, leg.fare_min, leg.fare_max, leg.note,
                       leg.checked_at, leg.status
                FROM transport_leg leg
                JOIN hub source ON source.id = leg.from_hub_id
                JOIN hub target ON target.id = leg.to_hub_id
                WHERE %s AND leg.%s
                ORDER BY leg.mode, leg.service_name
                """.formatted(hubFilter, VISIBLE))
                .param("hub", hubId)
                .query((rs, row) -> new TransportOption(
                        rs.getString("from_hub"),
                        rs.getString("to_hub"),
                        rs.getString("mode"),
                        rs.getString("service_name"),
                        rs.getObject("duration_min", Integer.class),
                        rs.getString("departure_note"),
                        rs.getObject("fare_min", Integer.class),
                        rs.getObject("fare_max", Integer.class),
                        rs.getString("note"),
                        toLocalDate(rs.getDate("checked_at")),
                        isVerified(rs)))
                .list();
    }

    private static DestinationSummary summary(ResultSet rs, int row) throws SQLException {
        return new DestinationSummary(
                rs.getString("slug"),
                rs.getString("name_en"),
                rs.getString("name_bn"),
                rs.getString("division_slug"),
                rs.getString("division_en"),
                rs.getString("division_bn"),
                isVerified(rs));
    }

    private static boolean isVerified(ResultSet rs) throws SQLException {
        return "PUBLISHED".equals(rs.getString("status"));
    }

    private static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    private static LocalDate toLocalDate(Date value) {
        return value != null ? value.toLocalDate() : null;
    }

    private static List<String> toList(Array value) throws SQLException {
        return Arrays.asList((String[]) value.getArray());
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public record DestinationRow(
            long id, Long arrivalHubId, DestinationSummary summary, Double lat, Double lng,
            Integer localTransportDaily, Integer foodDaily, String travelNote, String hotelNote,
            String universityNote, String guideUrl) {
    }
}
