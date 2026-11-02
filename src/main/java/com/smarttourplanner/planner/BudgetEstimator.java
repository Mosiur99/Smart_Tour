package com.smarttourplanner.planner;

import com.smarttourplanner.planner.PlannerModels.BudgetEstimate;
import com.smarttourplanner.planner.PlannerModels.CostCategory;
import com.smarttourplanner.planner.PlannerModels.CostRange;
import com.smarttourplanner.planner.PlannerModels.DayPlan;
import com.smarttourplanner.planner.PlannerModels.Hotel;
import com.smarttourplanner.planner.PlannerModels.Journey;
import com.smarttourplanner.planner.PlannerModels.Leg;
import com.smarttourplanner.planner.PlannerModels.MissingCost;
import com.smarttourplanner.planner.PlannerModels.ScheduledStop;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import java.util.ArrayList;
import java.util.List;

/** Adds up known costs as a range. Unknown prices are listed, never counted as zero. */
final class BudgetEstimator {

    private final List<CostRange> costs = new ArrayList<>();
    private final List<MissingCost> notIncluded = new ArrayList<>();

    private BudgetEstimator() {
    }

    static int roomsFor(int people, Hotel hotel) {
        return (people + hotel.roomCapacity() - 1) / hotel.roomCapacity();
    }

    static BudgetEstimate estimate(
            TripDestination destination, Journey outbound, Journey inbound, List<DayPlan> days, Hotel hotel,
            int people, int nights) {
        BudgetEstimator estimator = new BudgetEstimator();
        estimator.addTransport(outbound, inbound, people);
        estimator.addHotel(hotel, people, nights);
        estimator.addDaily(CostCategory.FOOD, destination.foodDaily(), people * days.size());
        estimator.addDaily(CostCategory.LOCAL_TRANSPORT, destination.localTransportDaily(), days.size());
        estimator.addEntryFees(days, people);
        return estimator.result();
    }

    private void addTransport(Journey outbound, Journey inbound, int people) {
        int low = 0;
        int high = 0;
        for (Journey journey : List.of(outbound, inbound)) {
            for (Leg leg : journey.legs()) {
                if (leg.fareMin() == null) {
                    notIncluded.add(new MissingCost(CostCategory.TRANSPORT, leg.serviceName(), null));
                } else {
                    low += leg.fareMin() * people;
                    high += leg.fareMax() * people;
                }
            }
        }
        add(CostCategory.TRANSPORT, low, high);
    }

    private void addHotel(Hotel hotel, int people, int nights) {
        if (hotel != null && nights > 0) {
            int roomNights = roomsFor(people, hotel) * nights;
            add(CostCategory.HOTEL, hotel.priceMin() * roomNights, hotel.priceMax() * roomNights);
        }
    }

    private void addDaily(CostCategory category, Integer dailyCost, int units) {
        if (dailyCost == null) {
            notIncluded.add(new MissingCost(category, null, null));
        } else {
            add(category, dailyCost * units, dailyCost * units);
        }
    }

    private void addEntryFees(List<DayPlan> days, int people) {
        int low = 0;
        int high = 0;
        for (DayPlan day : days) {
            for (ScheduledStop item : day.stops()) {
                if (item.stop().feeMin() == null) {
                    notIncluded.add(new MissingCost(CostCategory.ENTRY_FEES, item.stop().nameEn(), item.stop().nameBn()));
                } else {
                    low += item.stop().feeMin() * people;
                    high += feeHigh(item) * people;
                }
            }
        }
        add(CostCategory.ENTRY_FEES, low, high);
    }

    private int feeHigh(ScheduledStop item) {
        return item.stop().feeMax() != null ? item.stop().feeMax() : item.stop().feeMin();
    }

    private void add(CostCategory category, int low, int high) {
        if (high > 0) {
            costs.add(new CostRange(category, low, high));
        }
    }

    private BudgetEstimate result() {
        int low = costs.stream().mapToInt(CostRange::low).sum();
        int high = costs.stream().mapToInt(CostRange::high).sum();
        return new BudgetEstimate(low, high, List.copyOf(costs), List.copyOf(notIncluded));
    }
}
