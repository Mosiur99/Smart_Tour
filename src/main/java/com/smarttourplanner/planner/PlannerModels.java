package com.smarttourplanner.planner;

import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Inputs and results of the plan engine. A null price or fee means unknown, never zero. */
public final class PlannerModels {

    private PlannerModels() {
    }

    public enum BudgetLevel { BUDGET, MID, PREMIUM }

    public enum Interest { NATURE, HERITAGE, ADVENTURE, RELAXATION, CULTURE }

    public enum StopKind { SPOT, HERITAGE, MARKET, FOOD, CRAFT }

    public enum Notice { OFF_SEASON, SHORTER_THAN_RECOMMENDED, HOTEL_AT_OTHER_LEVEL, NO_HOTEL_AVAILABLE }

    public enum CostCategory { TRANSPORT, HOTEL, FOOD, LOCAL_TRANSPORT, ENTRY_FEES }

    public record PlanRequest(
            String originHub, String destination, int days, int people, BudgetLevel budget, int month,
            Set<Interest> interests) {
    }

    public record Leg(
            long id, String fromHub, String toHub, String mode, String serviceName, int durationMinutes,
            @Nullable Integer fareMin, @Nullable Integer fareMax, Set<Integer> months) {
    }

    public record Stop(
            long id, StopKind kind, String nameEn, String nameBn, @Nullable Double lat, @Nullable Double lng,
            int priority, @Nullable Integer visitMinutes, @Nullable Integer feeMin, @Nullable Integer feeMax,
            Set<String> tags) {
    }

    public record Hotel(
            long id, String nameEn, @Nullable String nameBn, BudgetLevel level, double lat, double lng,
            int roomCapacity, int priceMin, int priceMax) {
    }

    public record TripDestination(
            String slug, String nameEn, String nameBn, String arrivalHub, double lat, double lng, double roadFactor,
            Set<Integer> bestMonths, int minDays, @Nullable Integer foodDaily, @Nullable Integer localTransportDaily,
            List<Stop> stops, List<Hotel> hotels) {
    }

    public record Journey(List<Leg> legs, int totalMinutes) {
    }

    public record ScheduledStop(Stop stop, int travelMinutes) {
    }

    public record DayPlan(
            int day, int availableMinutes, int usedMinutes, List<ScheduledStop> stops, @Nullable Stop meal,
            @Nullable Stop market, @Nullable Stop craft) {
    }

    public record CostRange(CostCategory category, int low, int high) {
    }

    /** Something left out of the total. Names are null when the category alone says it, as for food. */
    public record MissingCost(CostCategory category, @Nullable String nameEn, @Nullable String nameBn) {
    }

    public record BudgetEstimate(int low, int high, List<CostRange> costs, List<MissingCost> notIncluded) {
    }

    public record TripPlan(
            String destination, String destinationNameEn, String destinationNameBn, int days, int nights, int people,
            Journey outbound, Journey inbound, List<DayPlan> dayPlans, @Nullable Hotel hotel, int rooms,
            BudgetEstimate budget, List<Notice> notices) {
    }
}
