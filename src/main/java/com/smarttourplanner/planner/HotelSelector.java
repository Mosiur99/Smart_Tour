package com.smarttourplanner.planner;

import com.smarttourplanner.planner.PlannerModels.BudgetLevel;
import com.smarttourplanner.planner.PlannerModels.DayPlan;
import com.smarttourplanner.planner.PlannerModels.Hotel;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the hotel closest to the trip's places, preferring the requested budget level. */
final class HotelSelector {

    private HotelSelector() {
    }

    static Optional<Hotel> select(TripDestination destination, List<DayPlan> days, BudgetLevel level) {
        double lat = average(days, destination.lat(), true);
        double lng = average(days, destination.lng(), false);
        Comparator<Hotel> closest = Comparator
                .comparingDouble((Hotel hotel) -> GeoDistance.kilometers(hotel.lat(), hotel.lng(), lat, lng))
                .thenComparingLong(Hotel::id);

        Optional<Hotel> atLevel = destination.hotels().stream().filter(hotel -> hotel.level() == level).min(closest);
        if (atLevel.isPresent()) {
            return atLevel;
        }
        return destination.hotels().stream()
                .min(Comparator.comparingInt((Hotel hotel) -> Math.abs(hotel.level().ordinal() - level.ordinal()))
                        .thenComparing(closest));
    }

    private static double average(List<DayPlan> days, double fallback, boolean latitude) {
        return days.stream()
                .flatMap(day -> day.stops().stream())
                .mapToDouble(item -> latitude ? item.stop().lat() : item.stop().lng())
                .average()
                .orElse(fallback);
    }
}
