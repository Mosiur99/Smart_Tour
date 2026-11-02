package com.smarttourplanner.admin;

import static com.smarttourplanner.admin.Field.choice;
import static com.smarttourplanner.admin.Field.date;
import static com.smarttourplanner.admin.Field.decimal;
import static com.smarttourplanner.admin.Field.hub;
import static com.smarttourplanner.admin.Field.integer;
import static com.smarttourplanner.admin.Field.longText;
import static com.smarttourplanner.admin.Field.numbers;
import static com.smarttourplanner.admin.Field.tags;
import static com.smarttourplanner.admin.Field.text;
import static com.smarttourplanner.admin.Field.url;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The kinds of records the admin panel edits. Table and column names come only from here, never
 * from a request, so the SQL built from them can't be changed by what someone sends.
 */
enum Editable {

    DESTINATION("destinations", "destination", "t.name_en", "t.slug", List.of(
            text("nameEn", "name_en").notEmpty(),
            text("nameBn", "name_bn"),
            hub("arrivalHub", "arrival_hub_id"),
            decimal("lat", "lat", "20", "27"),
            decimal("lng", "lng", "88", "93"),
            decimal("roadFactor", "road_factor", "1", "3").notEmpty(),
            numbers("bestMonths", "best_months", 1, 12).notEmpty(),
            integer("minDays", "min_days", 1, 14).notEmpty(),
            integer("localTransportDaily", "local_transport_daily", 0, 100_000),
            integer("foodDaily", "food_daily", 0, 100_000),
            longText("travelNote", "travel_note"),
            longText("hotelNote", "hotel_note"),
            longText("universityNote", "university_note"),
            url("guideUrl", "guide_url"))),

    PLACE("places", "poi", "t.name_en", Sql.OWNER_SLUG, List.of(
            choice("kind", "kind", "poi_kind", List.of("SPOT", "HERITAGE", "MARKET", "FOOD", "CRAFT")).notEmpty(),
            text("nameEn", "name_en").notEmpty(),
            text("nameBn", "name_bn"),
            decimal("lat", "lat", "20", "27"),
            decimal("lng", "lng", "88", "93"),
            integer("priority", "priority", 0, 1_000).notEmpty(),
            integer("visitMinutes", "visit_minutes", 1, 1_440),
            integer("feeMin", "fee_min", 0, 1_000_000),
            integer("feeMax", "fee_max", 0, 1_000_000),
            tags("tags", "tags"),
            longText("note", "note"),
            url("sourceUrl", "source_url"),
            date("checkedAt", "checked_at"))),

    HOTEL("hotels", "hotel", "t.name_en", Sql.OWNER_SLUG, List.of(
            text("nameEn", "name_en").notEmpty(),
            text("nameBn", "name_bn"),
            choice("tier", "tier", "hotel_tier", List.of("BUDGET", "MID", "PREMIUM")),
            text("address", "address"),
            decimal("lat", "lat", "20", "27"),
            decimal("lng", "lng", "88", "93"),
            integer("roomCapacity", "room_capacity", 1, 20).notEmpty(),
            integer("priceMin", "price_min", 1, 1_000_000),
            integer("priceMax", "price_max", 1, 1_000_000),
            url("website", "website"),
            longText("note", "note"),
            url("sourceUrl", "source_url"),
            date("checkedAt", "checked_at"))),

    LEG("legs", "transport_leg", Sql.LEG_LABEL, Sql.LEG_DESTINATION, List.of(
            hub("fromHub", "from_hub_id").notEmpty().fixedOnceCreated(),
            hub("toHub", "to_hub_id").notEmpty().fixedOnceCreated(),
            choice("mode", "mode", "travel_mode",
                    List.of("BUS", "TRAIN", "LAUNCH", "SHIP", "FLIGHT", "JEEP", "BOAT")).notEmpty(),
            text("serviceName", "service_name").notEmpty(),
            integer("durationMinutes", "duration_min", 1, 4_320),
            text("departureNote", "departure_note"),
            integer("fareMin", "fare_min", 1, 1_000_000),
            integer("fareMax", "fare_max", 1, 1_000_000),
            numbers("months", "months", 1, 12).notEmpty(),
            longText("note", "note"),
            url("sourceUrl", "source_url"),
            date("checkedAt", "checked_at")));

    final String path;
    final String table;
    /** SQL for a short name, read from the row aliased {@code t}. */
    final String labelSql;
    /** SQL for the slug of the destination the record is edited under. */
    final String destinationSql;
    final List<Field> fields;

    Editable(String path, String table, String labelSql, String destinationSql, List<Field> fields) {
        this.path = path;
        this.table = table;
        this.labelSql = labelSql;
        this.destinationSql = destinationSql;
        this.fields = fields;
    }

    /** Kept apart, because enum constants can't use the enum's own constants. */
    private static final class Sql {
        static final String OWNER_SLUG = "(SELECT slug FROM destination WHERE id = t.destination_id)";
        static final String LEG_LABEL = "(SELECT name_en FROM hub WHERE id = t.from_hub_id) || ' to '"
                + " || (SELECT name_en FROM hub WHERE id = t.to_hub_id) || ', ' || t.service_name";
        // A journey shows under the destination it arrives at, or else the one it leaves from.
        static final String LEG_DESTINATION = "(SELECT slug FROM destination d"
                + " WHERE d.arrival_hub_id IN (t.to_hub_id, t.from_hub_id)"
                + " ORDER BY d.arrival_hub_id = t.to_hub_id DESC, d.slug LIMIT 1)";
    }

    static Optional<Editable> fromPath(String path) {
        return Stream.of(values()).filter(kind -> kind.path.equals(path)).findFirst();
    }

    Optional<Field> field(String name) {
        return fields.stream().filter(field -> field.name().equals(name)).findFirst();
    }

    /** Places and hotels belong to a destination. Journeys connect two hubs instead. */
    boolean belongsToDestination() {
        return this == PLACE || this == HOTEL;
    }
}
