package com.smarttourplanner.planner;

import com.smarttourplanner.planner.PlannerModels.BudgetLevel;
import com.smarttourplanner.planner.PlannerModels.Hotel;
import com.smarttourplanner.planner.PlannerModels.Leg;
import com.smarttourplanner.planner.PlannerModels.PlanRequest;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.StopKind;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import java.util.List;
import java.util.Set;

/** Made-up travel data for planner tests. */
final class PlanFixtures {

    static final Set<Integer> ALL_YEAR = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

    private PlanFixtures() {
    }

    static Leg leg(long id, String from, String to, int minutes, Integer fareMin, Integer fareMax) {
        return new Leg(id, from, to, "BUS", "Service " + id, minutes, fareMin, fareMax, ALL_YEAR);
    }

    static Leg seasonalLeg(long id, String from, String to, int minutes, Set<Integer> months) {
        return new Leg(id, from, to, "SHIP", "Service " + id, minutes, 500, 500, months);
    }

    static Stop spot(long id, int priority, double lat, double lng, int visitMinutes, Integer fee) {
        return new Stop(id, StopKind.SPOT, "Spot " + id, "স্থান " + id, lat, lng, priority, visitMinutes, fee, fee, Set.of());
    }

    static Stop tagged(long id, int priority, String tag) {
        return new Stop(id, StopKind.SPOT, "Spot " + id, null, 22.0, 92.0, priority, 60, 0, 0, Set.of(tag));
    }

    static Stop extra(long id, StopKind kind, Double lat, Double lng) {
        return new Stop(id, kind, "Extra " + id, null, lat, lng, 100, null, null, null, Set.of());
    }

    static Hotel hotel(long id, BudgetLevel level, double lat, double lng, int capacity, int min, int max) {
        return new Hotel(id, "Hotel " + id, null, level, lat, lng, capacity, min, max);
    }

    static TripDestination destination(List<Stop> stops, List<Hotel> hotels) {
        return destination(stops, hotels, ALL_YEAR, 1, 800, 1000);
    }

    static TripDestination destination(
            List<Stop> stops, List<Hotel> hotels, Set<Integer> months, int minDays, Integer food, Integer local) {
        return new TripDestination(
                "hill-town", "Hill Town", "পাহাড়ি শহর", "hill-hub", 22.0, 92.0, 1.4, months, minDays, food, local, stops, hotels);
    }

    static PlanRequest request(int days, int people, BudgetLevel budget) {
        return new PlanRequest("capital", "hill-town", days, people, budget, 12, Set.of());
    }
}
