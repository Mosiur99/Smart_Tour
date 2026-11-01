package db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * One-time import of the original JSON data. Reads raw JSON instead of the app's model classes,
 * so later model changes can't alter what this migration does.
 */
public class V2__Import_catalog extends BaseJavaMigration {

    private static final String HUB_FROM_DHAKA = "dhaka";
    private static final Pattern FARE_RANGE = Pattern.compile("(\\d+)-(\\d+)");

    private final JsonMapper mapper = JsonMapper.builder().build();
    private JdbcTemplate jdbc;

    @Override
    public void migrate(Context context) throws IOException {
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        JsonNode index = read("data/districts-index.json");
        JsonNode banglaNames = read("data/names-bn.json");

        importDivisions(index, banglaNames.get("divisions"));
        for (JsonNode summary : index) {
            importDistrict(summary, banglaNames.get("districts"));
        }
        for (JsonNode summary : index) {
            importTransport(read(districtPath(summary)), summary.get("id").asString());
        }
    }

    private void importDivisions(JsonNode index, JsonNode banglaNames) {
        List<String> seen = new ArrayList<>();
        for (JsonNode summary : index) {
            String name = summary.get("division").asString();
            if (!seen.contains(name)) {
                seen.add(name);
                jdbc.update("INSERT INTO division (slug, name_en, name_bn) VALUES (?, ?, ?)",
                        name.toLowerCase(), name, banglaNames.get(name).asString());
            }
        }
    }

    private void importDistrict(JsonNode summary, JsonNode banglaNames) throws IOException {
        String slug = summary.get("id").asString();
        String name = summary.get("name").asString();
        String nameBn = banglaNames.get(slug).asString();
        JsonNode data = read(districtPath(summary));

        Long divisionId = jdbc.queryForObject("SELECT id FROM division WHERE name_en = ?", Long.class,
                summary.get("division").asString());
        Long districtId = insertReturningId(
                "INSERT INTO district (slug, division_id, name_en, name_bn) VALUES (?, ?, ?, ?) RETURNING id",
                slug, divisionId, name, nameBn);
        Long hubId = insertReturningId(
                "INSERT INTO hub (slug, district_id, name_en, name_bn) VALUES (?, ?, ?, ?) RETURNING id",
                slug, districtId, name, nameBn);
        Long destinationId = insertReturningId("""
                INSERT INTO destination (slug, district_id, arrival_hub_id, name_en, name_bn,
                    local_transport_daily, food_daily, travel_note, hotel_note, guide_url, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'IMPORTED') RETURNING id
                """,
                slug, districtId, hubId, name, nameBn,
                data.path("budgetDefaults").path("localTransportPerPerson").asInt(),
                data.path("budgetDefaults").path("foodPerPerson").asInt(),
                text(data, "travelNote"), text(data, "hotelNote"), text(data, "tourGuideUrl"));

        importSpots(destinationId, data.path("spots"), "SPOT");
        importSpots(destinationId, data.path("historicalPlaces"), "HERITAGE");
        importFoods(destinationId, data.path("foods"));
        importHotels(destinationId, data.path("hotels"));
    }

    private void importSpots(Long destinationId, JsonNode spots, String kind) {
        for (JsonNode spot : spots) {
            jdbc.update("""
                    INSERT INTO poi (destination_id, kind, name_en, priority, tags, note, status)
                    VALUES (?, ?::poi_kind, ?, ?, string_to_array(?, ','), ?, 'IMPORTED')
                    ON CONFLICT DO NOTHING
                    """,
                    destinationId, kind, spot.get("name").asString(), spot.path("priority").asInt(100),
                    text(spot, "area"), spotNote(spot));
        }
    }

    private String spotNote(JsonNode spot) {
        String tip = text(spot, "tip");
        String offDay = text(spot, "offDay");
        if (offDay == null) {
            return tip;
        }
        return tip == null ? "Closed: " + offDay : tip + " Closed: " + offDay;
    }

    private void importFoods(Long destinationId, JsonNode foods) {
        for (JsonNode food : foods) {
            jdbc.update("""
                    INSERT INTO poi (destination_id, kind, name_en, note, status)
                    VALUES (?, 'FOOD', ?, ?, 'IMPORTED')
                    ON CONFLICT DO NOTHING
                    """,
                    destinationId, food.get("name").asString(), text(food, "location"));
        }
    }

    private void importHotels(Long destinationId, JsonNode hotels) {
        for (JsonNode hotel : hotels) {
            Integer acPrice = number(hotel, "acPrice");
            Integer nonAcPrice = number(hotel, "nonAcPrice");
            jdbc.update("""
                    INSERT INTO hotel (destination_id, name_en, address, price_min, price_max,
                        website, note, source_url, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'IMPORTED')
                    ON CONFLICT DO NOTHING
                    """,
                    destinationId, hotel.get("name").asString(), text(hotel, "location"),
                    lowest(acPrice, nonAcPrice), highest(acPrice, nonAcPrice),
                    text(hotel, "website"), text(hotel, "review"), text(hotel, "website"));
        }
    }

    private void importTransport(JsonNode data, String districtSlug) {
        if (HUB_FROM_DHAKA.equals(districtSlug)) {
            return;
        }
        importLegs(data.path("transport").path("fromDhaka"), HUB_FROM_DHAKA, districtSlug);
        importLegs(data.path("transport").path("returnToDhaka"), districtSlug, HUB_FROM_DHAKA);
    }

    private void importLegs(JsonNode options, String fromSlug, String toSlug) {
        for (JsonNode option : options) {
            Integer fareMin = number(option, "costPerPerson");
            jdbc.update("""
                    INSERT INTO transport_leg (from_hub_id, to_hub_id, mode, service_name,
                        departure_note, fare_min, fare_max, note, status)
                    VALUES ((SELECT id FROM hub WHERE slug = ?), (SELECT id FROM hub WHERE slug = ?),
                        ?::travel_mode, ?, ?, ?, ?, ?, 'IMPORTED')
                    ON CONFLICT DO NOTHING
                    """,
                    fromSlug, toSlug, option.get("mode").asString().toUpperCase(),
                    option.get("name").asString(), text(option, "departureTime"),
                    fareMin, fareMax(option, fareMin), text(option, "note"));
        }
    }

    private Integer fareMax(JsonNode option, Integer fareMin) {
        return Optional.ofNullable(text(option, "costEstimateNote"))
                .map(FARE_RANGE::matcher)
                .filter(Matcher::find)
                .map(range -> Integer.valueOf(range.group(2)))
                .orElse(fareMin);
    }

    private Long insertReturningId(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private JsonNode read(String path) throws IOException {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return mapper.readTree(input);
        }
    }

    private static String districtPath(JsonNode summary) {
        return "data/districts/" + summary.get("id").asString() + ".json";
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() || value.asString().isBlank() ? null : value.asString();
    }

    private static Integer number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static Integer lowest(Integer first, Integer second) {
        if (first == null || second == null) {
            return first != null ? first : second;
        }
        return Math.min(first, second);
    }

    private static Integer highest(Integer first, Integer second) {
        if (first == null || second == null) {
            return first != null ? first : second;
        }
        return Math.max(first, second);
    }
}
