package com.smarttourplanner.planner;

import java.util.List;
import java.util.Set;

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
            Integer fareMin, Integer fareMax, Set<Integer> months) {
    }

    public record Stop(
            long id, StopKind kind, String nameEn, String nameBn, Double lat, Double lng, int priority,
            Integer visitMinutes, Integer feeMin, Integer feeMax, Set<String> tags) {
    }

    public record Hotel(
            long id, String nameEn, String nameBn, BudgetLevel level, double lat, double lng, int roomCapacity,
            int priceMin, int priceMax) {
    }

    public record TripDestination(
            String slug, String nameEn, String nameBn, String arrivalHub, double lat, double lng, double roadFactor,
            Set<Integer> bestMonths, int minDays, Integer foodDaily, Integer localTransportDaily,
            List<Stop> stops, List<Hotel> hotels) {
    }

    public record Journey(List<Leg> legs, int totalMinutes) {
    }

    public record ScheduledStop(Stop stop, int travelMinutes) {
    }

    public record DayPlan(
            int day, int availableMinutes, int usedMinutes, List<ScheduledStop> stops, Stop meal, Stop market,
            Stop craft) {
    }

    public record CostRange(CostCategory category, int low, int high) {
    }

    public record MissingCost(CostCategory category, String item) {
    }

    public record BudgetEstimate(int low, int high, List<CostRange> costs, List<MissingCost> notIncluded) {
    }

    public record TripPlan(
            String destination, String destinationNameEn, String destinationNameBn, int days, int nights, int people,
            Journey outbound, Journey inbound, List<DayPlan> dayPlans, Hotel hotel, int rooms,
            BudgetEstimate budget, List<Notice> notices) {
    }
}
