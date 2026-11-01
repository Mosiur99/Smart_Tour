package com.smarttourplanner.service;

import com.smarttourplanner.model.BudgetBreakdown;
import com.smarttourplanner.model.BudgetDefaults;
import com.smarttourplanner.model.Hotel;
import com.smarttourplanner.model.TourPlan;
import com.smarttourplanner.model.TransportOption;
import org.springframework.stereotype.Service;

@Service
public class BudgetCalculatorService {

    private static final int TRAVELERS_PER_ROOM = 2;

    public BudgetBreakdown calculate(TourPlan plan, BudgetDefaults defaults) {
        int travelers = Math.max(plan.getTravelers(), 1);
        int nights = Math.max(plan.getNights(), 1);

        BudgetBreakdown breakdown = new BudgetBreakdown();
        addTransport(breakdown, plan, travelers);
        addHotel(breakdown, plan, travelers, nights);
        addDailyCosts(breakdown, defaults, travelers, nights);
        breakdown.setTotal(
                breakdown.getTransportCost()
                        + breakdown.getHotelCost()
                        + breakdown.getLocalTransportCost()
                        + breakdown.getFoodCost());
        return breakdown;
    }

    private void addTransport(BudgetBreakdown breakdown, TourPlan plan, int travelers) {
        int outbound = fareOrRecordMissing(breakdown, plan.getOutboundTransport(), "Outbound fare");
        int inbound = fareOrRecordMissing(breakdown, plan.getReturnTransport(), "Return fare");

        breakdown.setTransportCost((outbound + inbound) * travelers);
        breakdown.setTransportDetail(
                String.format("(%d + %d) × %d travelers (estimated base fares)", outbound, inbound, travelers));
    }

    private int fareOrRecordMissing(BudgetBreakdown breakdown, TransportOption transport, String label) {
        if (transport == null || transport.getCostPerPerson() == null) {
            breakdown.addMissingCost(label);
            return 0;
        }
        return transport.getCostPerPerson();
    }

    private void addHotel(BudgetBreakdown breakdown, TourPlan plan, int travelers, int nights) {
        Hotel hotel = plan.getSelectedHotel();
        if (hotel == null) {
            breakdown.setHotelDetail("No hotel selected");
            return;
        }

        Integer pricePerNight = hotel.getPrice(plan.isAcRoom());
        if (pricePerNight == null) {
            breakdown.addMissingCost("Hotel");
            breakdown.setHotelDetail(hotel.getName() + " (price TBD — verify before booking)");
            return;
        }

        int rooms = roomsFor(travelers);
        breakdown.setHotelCost(pricePerNight * rooms * nights);
        breakdown.setHotelDetail(
                String.format("%s (%s) × %d room(s) × %d night(s)",
                        hotel.getName(), plan.isAcRoom() ? "AC" : "Non-AC", rooms, nights));
    }

    private int roomsFor(int travelers) {
        return (travelers + TRAVELERS_PER_ROOM - 1) / TRAVELERS_PER_ROOM;
    }

    private void addDailyCosts(BudgetBreakdown breakdown, BudgetDefaults defaults, int travelers, int nights) {
        int localTransport = defaults.getLocalTransportPerPerson();
        breakdown.setLocalTransportCost(localTransport * travelers * nights);
        breakdown.setLocalTransportDetail(
                String.format("%d × %d travelers × %d night(s)", localTransport, travelers, nights));

        int food = defaults.getFoodPerPerson();
        breakdown.setFoodCost(food * travelers * nights);
        breakdown.setFoodDetail(String.format("%d × %d travelers × %d night(s)", food, travelers, nights));
    }
}
