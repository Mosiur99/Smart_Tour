package db.migration;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads researched data for the first five routes from data/routes. Everything arrives unchecked:
 * new records are drafts, and no checked date is set, so nothing can be published until a person
 * has checked it in the admin panel. Records a person has already published are never changed.
 */
public class V7__Add_route_data extends BaseJavaMigration {

    static final List<String> ROUTES = List.of("coxs-bazar", "saint-martin", "sylhet", "bandarban", "sajek");

    private static final String NOT_PUBLISHED = "status <> 'PUBLISHED'";

    private final JsonMapper mapper = JsonMapper.builder().build();
    private JdbcTemplate jdbc;

    @Override
    public void migrate(Context context) throws IOException {
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        for (String route : ROUTES) {
            JsonNode data = read("data/routes/" + route + ".json");
            data.path("hubs").forEach(this::addHub);
            JsonNode destination = data.get("destination");
            saveDestination(destination);
            long destinationId = jdbc.queryForObject(
                    "SELECT id FROM destination WHERE slug = ?", Long.class, destination.get("slug").asString());
            data.path("archive").forEach(entry -> archive(destinationId, entry));
            data.path("places").forEach(place -> savePlace(destinationId, place));
            data.path("hotels").forEach(hotel -> saveHotel(destinationId, hotel));
            data.path("legs").forEach(this::saveLeg);
        }
        jdbc.update("UPDATE catalog_version SET version = version + 1");
    }

    private void addHub(JsonNode hub) {
        jdbc.update("""
                INSERT INTO hub (slug, district_id, kind, name_en, name_bn, lat, lng)
                VALUES (?, (SELECT id FROM district WHERE slug = ?), ?, ?, ?, ?, ?)
                ON CONFLICT (slug) DO NOTHING
                """,
                text(hub, "slug"), text(hub, "district"), text(hub, "kind"), text(hub, "nameEn"),
                text(hub, "nameBn"), decimal(hub, "lat"), decimal(hub, "lng"));
    }

    private void saveDestination(JsonNode destination) {
        Object[] values = {
            text(destination, "nameEn"), text(destination, "nameBn"), text(destination, "arrivalHub"),
            decimal(destination, "lat"), decimal(destination, "lng"), decimal(destination, "roadFactor"),
            list(destination, "bestMonths"), number(destination, "minDays"), number(destination, "foodDaily"),
            number(destination, "localTransportDaily"), text(destination, "travelNote"),
            text(destination, "hotelNote"), text(destination, "slug")
        };
        if (destination.path("create").asBoolean(false)) {
            jdbc.update("""
                    INSERT INTO destination (name_en, name_bn, arrival_hub_id, lat, lng, road_factor, best_months,
                        min_days, food_daily, local_transport_daily, travel_note, hotel_note, slug, district_id, status)
                    VALUES (?, ?, (SELECT id FROM hub WHERE slug = ?), ?, ?, ?, string_to_array(?, ',')::smallint[],
                        ?, ?, ?, ?, ?, ?, (SELECT id FROM district WHERE slug = ?), 'DRAFT')
                    ON CONFLICT (slug) DO NOTHING
                    """, append(values, text(destination, "district")));
        } else {
            jdbc.update("""
                    UPDATE destination SET name_en = ?, name_bn = ?, arrival_hub_id = (SELECT id FROM hub WHERE slug = ?),
                        lat = ?, lng = ?, road_factor = ?, best_months = string_to_array(?, ',')::smallint[],
                        min_days = ?, food_daily = ?, local_transport_daily = ?, travel_note = ?,
                        hotel_note = coalesce(?, hotel_note)
                    WHERE slug = ? AND\s""" + NOT_PUBLISHED, values);
        }
    }

    private void archive(long destinationId, JsonNode entry) {
        jdbc.update("UPDATE poi SET status = 'ARCHIVED' WHERE destination_id = ? AND kind = ?::poi_kind AND name_en = ? AND "
                + NOT_PUBLISHED, destinationId, text(entry, "kind"), text(entry, "nameEn"));
    }

    private void savePlace(long destinationId, JsonNode place) {
        jdbc.update("""
                INSERT INTO poi (destination_id, kind, name_en, name_bn, lat, lng, priority, visit_minutes,
                    fee_min, fee_max, tags, note, source_url, status)
                VALUES (?, ?::poi_kind, ?, ?, ?, ?, ?, ?, ?, ?, string_to_array(?, '|'), ?, ?, 'DRAFT')
                ON CONFLICT (destination_id, kind, name_en) DO UPDATE SET
                    name_bn = excluded.name_bn, lat = excluded.lat, lng = excluded.lng, priority = excluded.priority,
                    visit_minutes = excluded.visit_minutes, fee_min = excluded.fee_min, fee_max = excluded.fee_max,
                    tags = excluded.tags, note = excluded.note, source_url = excluded.source_url
                WHERE poi.""" + NOT_PUBLISHED,
                destinationId, text(place, "kind"), text(place, "nameEn"), text(place, "nameBn"),
                decimal(place, "lat"), decimal(place, "lng"), place.path("priority").asInt(100),
                number(place, "visitMinutes"), number(place, "feeMin"), number(place, "feeMax"),
                String.join("|", strings(place, "tags")), text(place, "note"), text(place, "sourceUrl"));
    }

    private void saveHotel(long destinationId, JsonNode hotel) {
        jdbc.update("""
                INSERT INTO hotel (destination_id, name_en, name_bn, tier, address, lat, lng, room_capacity,
                    price_min, price_max, website, note, source_url, status)
                VALUES (?, ?, ?, ?::hotel_tier, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                ON CONFLICT (destination_id, name_en) DO UPDATE SET
                    name_bn = excluded.name_bn, tier = excluded.tier, address = excluded.address, lat = excluded.lat,
                    lng = excluded.lng, room_capacity = excluded.room_capacity, price_min = excluded.price_min,
                    price_max = excluded.price_max, website = excluded.website, note = excluded.note,
                    source_url = excluded.source_url
                WHERE hotel.""" + NOT_PUBLISHED,
                destinationId, text(hotel, "nameEn"), text(hotel, "nameBn"), text(hotel, "tier"),
                text(hotel, "address"), decimal(hotel, "lat"), decimal(hotel, "lng"),
                hotel.path("roomCapacity").asInt(2), number(hotel, "priceMin"), number(hotel, "priceMax"),
                text(hotel, "website"), text(hotel, "note"), text(hotel, "sourceUrl"));
    }

    private void saveLeg(JsonNode leg) {
        List<String> months = strings(leg, "months");
        jdbc.update("""
                INSERT INTO transport_leg (from_hub_id, to_hub_id, mode, service_name, duration_min, departure_note,
                    fare_min, fare_max, months, note, source_url, status)
                VALUES ((SELECT id FROM hub WHERE slug = ?), (SELECT id FROM hub WHERE slug = ?), ?::travel_mode, ?,
                    ?, ?, ?, ?, string_to_array(?, ',')::smallint[], ?, ?, 'DRAFT')
                ON CONFLICT (from_hub_id, to_hub_id, mode, service_name) DO UPDATE SET
                    duration_min = excluded.duration_min, departure_note = excluded.departure_note,
                    fare_min = excluded.fare_min, fare_max = excluded.fare_max, months = excluded.months,
                    note = excluded.note, source_url = excluded.source_url
                WHERE transport_leg.""" + NOT_PUBLISHED,
                text(leg, "from"), text(leg, "to"), text(leg, "mode"), text(leg, "serviceName"),
                number(leg, "durationMinutes"), text(leg, "departureNote"), number(leg, "fareMin"),
                number(leg, "fareMax"), months.isEmpty() ? "1,2,3,4,5,6,7,8,9,10,11,12" : String.join(",", months),
                text(leg, "note"), text(leg, "sourceUrl"));
    }

    private JsonNode read(String path) throws IOException {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return mapper.readTree(input);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static Integer number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    private static List<String> strings(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        node.path(field).forEach(item -> values.add(item.asString()));
        return values;
    }

    private static String list(JsonNode node, String field) {
        return String.join(",", strings(node, field));
    }

    private static Object[] append(Object[] values, Object last) {
        Object[] all = Arrays.copyOf(values, values.length + 1);
        all[values.length] = last;
        return all;
    }
}
