package com.smarttourplanner.plans;

import com.smarttourplanner.planner.PlannerModels.BudgetLevel;
import com.smarttourplanner.planner.PlannerModels.Hotel;
import com.smarttourplanner.planner.PlannerModels.Leg;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.StopKind;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Loads only PUBLISHED records, so plans never use unverified data. */
@Repository
class PlanningDataRepository {

    private final JdbcClient jdbc;

    PlanningDataRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    boolean hubExists(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM hub WHERE slug = :slug)")
                .param("slug", slug)
                .query(Boolean.class)
                .single();
    }

    List<Leg> findPublishedLegs() {
        return jdbc.sql("""
                SELECT leg.id, source.slug AS from_hub, target.slug AS to_hub, leg.mode, leg.service_name,
                       leg.duration_min, leg.fare_min, leg.fare_max, leg.months
                FROM transport_leg leg
                JOIN hub source ON source.id = leg.from_hub_id
                JOIN hub target ON target.id = leg.to_hub_id
                WHERE leg.status = 'PUBLISHED'
                """)
                .query((rs, row) -> new Leg(
                        rs.getLong("id"), rs.getString("from_hub"), rs.getString("to_hub"), rs.getString("mode"),
                        rs.getString("service_name"), rs.getInt("duration_min"),
                        rs.getObject("fare_min", Integer.class), rs.getObject("fare_max", Integer.class),
                        months(rs.getArray("months"))))
                .list();
    }

    Optional<TripDestination> findPublishedDestination(String slug) {
        return jdbc.sql("""
                SELECT d.id, d.slug, d.name_en, d.name_bn, hub.slug AS arrival_hub, d.lat, d.lng, d.road_factor,
                       d.best_months, d.min_days, d.food_daily, d.local_transport_daily
                FROM destination d
                JOIN hub ON hub.id = d.arrival_hub_id
                WHERE d.slug = :slug AND d.status = 'PUBLISHED'
                """)
                .param("slug", slug)
                .query((rs, row) -> {
                    long id = rs.getLong("id");
                    return new TripDestination(
                            rs.getString("slug"), rs.getString("name_en"), rs.getString("name_bn"),
                            rs.getString("arrival_hub"), rs.getBigDecimal("lat").doubleValue(),
                            rs.getBigDecimal("lng").doubleValue(), rs.getBigDecimal("road_factor").doubleValue(),
                            months(rs.getArray("best_months")), rs.getInt("min_days"),
                            rs.getObject("food_daily", Integer.class),
                            rs.getObject("local_transport_daily", Integer.class),
                            findStops(id), findHotels(id));
                })
                .optional();
    }

    private List<Stop> findStops(long destinationId) {
        return jdbc.sql("""
                SELECT id, kind, name_en, name_bn, lat, lng, priority, visit_minutes, fee_min, fee_max, tags
                FROM poi
                WHERE destination_id = :destination AND status = 'PUBLISHED'
                """)
                .param("destination", destinationId)
                .query((rs, row) -> new Stop(
                        rs.getLong("id"), StopKind.valueOf(rs.getString("kind")), rs.getString("name_en"),
                        rs.getString("name_bn"), toDouble(rs.getBigDecimal("lat")), toDouble(rs.getBigDecimal("lng")),
                        rs.getInt("priority"), rs.getObject("visit_minutes", Integer.class),
                        rs.getObject("fee_min", Integer.class), rs.getObject("fee_max", Integer.class),
                        tags(rs.getArray("tags"))))
                .list();
    }

    private List<Hotel> findHotels(long destinationId) {
        return jdbc.sql("""
                SELECT id, name_en, name_bn, tier, lat, lng, room_capacity, price_min, price_max
                FROM hotel
                WHERE destination_id = :destination AND status = 'PUBLISHED'
                """)
                .param("destination", destinationId)
                .query(PlanningDataRepository::hotel)
                .list();
    }

    private static Hotel hotel(ResultSet rs, int row) throws SQLException {
        return new Hotel(
                rs.getLong("id"), rs.getString("name_en"), rs.getString("name_bn"),
                BudgetLevel.valueOf(rs.getString("tier")), rs.getBigDecimal("lat").doubleValue(),
                rs.getBigDecimal("lng").doubleValue(), rs.getInt("room_capacity"), rs.getInt("price_min"),
                rs.getInt("price_max"));
    }

    private static Set<Integer> months(Array value) throws SQLException {
        return Arrays.stream((Object[]) value.getArray())
                .map(month -> ((Number) month).intValue())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> tags(Array value) throws SQLException {
        return Set.of((String[]) value.getArray());
    }

    private static Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
