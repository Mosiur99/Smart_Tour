package com.smarttourplanner.planner;

import com.smarttourplanner.planner.PlannerModels.BudgetEstimate;
import com.smarttourplanner.planner.PlannerModels.DayPlan;
import com.smarttourplanner.planner.PlannerModels.Hotel;
import com.smarttourplanner.planner.PlannerModels.Journey;
import com.smarttourplanner.planner.PlannerModels.Leg;
import com.smarttourplanner.planner.PlannerModels.Notice;
import com.smarttourplanner.planner.PlannerModels.PlanRequest;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import com.smarttourplanner.planner.PlannerModels.TripPlan;
import java.util.ArrayList;
import java.util.List;

/** Builds a travel plan. It's plain Java, and the same input always gives the same plan. */
public class PlanEngine {

    private final RouteFinder routes = new RouteFinder();

    public TripPlan plan(PlanRequest request, TripDestination destination, List<Leg> legs) {
        Journey outbound = journey(legs, request.originHub(), destination.arrivalHub(), request.month());
        Journey inbound = journey(legs, destination.arrivalHub(), request.originHub(), request.month());

        List<Integer> dayMinutes = TripTimeline.availableMinutes(
                request.days(), outbound.totalMinutes(), inbound.totalMinutes());
        List<DayPlan> days = new DayScheduler(destination)
                .schedule(StopRanker.rank(destination.stops(), request.interests()), dayMinutes);

        int nights = request.days() - 1;
        Hotel hotel = nights > 0 ? HotelSelector.select(destination, days, request.budget()).orElse(null) : null;
        int rooms = hotel != null ? BudgetEstimator.roomsFor(request.people(), hotel) : 0;
        BudgetEstimate budget = BudgetEstimator.estimate(
                destination, outbound, inbound, days, hotel, request.people(), nights);

        return new TripPlan(
                destination.slug(), destination.nameEn(), destination.nameBn(), request.days(), nights,
                request.people(), outbound, inbound, days, hotel, rooms, budget,
                notices(request, destination, hotel, nights));
    }

    private Journey journey(List<Leg> legs, String fromHub, String toHub, int month) {
        return routes.fastest(legs, fromHub, toHub, month)
                .orElseThrow(() -> new NoRouteException(fromHub, toHub, month));
    }

    private List<Notice> notices(PlanRequest request, TripDestination destination, Hotel hotel, int nights) {
        List<Notice> notices = new ArrayList<>();
        if (!destination.bestMonths().contains(request.month())) {
            notices.add(Notice.OFF_SEASON);
        }
        if (request.days() < destination.minDays()) {
            notices.add(Notice.SHORTER_THAN_RECOMMENDED);
        }
        if (nights > 0 && hotel == null) {
            notices.add(Notice.NO_HOTEL_AVAILABLE);
        }
        if (hotel != null && hotel.level() != request.budget()) {
            notices.add(Notice.HOTEL_AT_OTHER_LEVEL);
        }
        return List.copyOf(notices);
    }
}
