package com.smarttourplanner.service;

import com.smarttourplanner.model.BudgetBreakdown;
import com.smarttourplanner.model.BudgetDefaults;
import com.smarttourplanner.model.Hotel;
import com.smarttourplanner.model.TourPlan;
import com.smarttourplanner.model.TransportOption;
import org.springframework.stereotype.Service;

@Service
public class BudgetCalculatorService {

    public BudgetBreakdown calculate(TourPlan plan, BudgetDefaults defaults) {
        BudgetBreakdown breakdown = new BudgetBreakdown();

        int outboundCost = getTransportCost(plan.getOutboundTransport());
        int returnCost = getTransportCost(plan.getReturnTransport());
        int travelers = Math.max(plan.getTravelers(), 1);
        int nights = Math.max(plan.getNights(), 1);

        breakdown.setTrainCost((outboundCost + returnCost) * travelers);
        breakdown.setTrainDetail(
                String.format("(%d + %d) × %d travelers (estimated base fares)", outboundCost, returnCost, travelers));

        Hotel hotel = plan.getSelectedHotel();
        int hotelPrice = 0;
        if (hotel != null) {
            hotelPrice = plan.isAcRoom() ? hotel.getAcPriceOrZero() : hotel.getNonAcPriceOrZero();
            if (hotelPrice == 0 && !hotel.hasPrice()) {
                breakdown.setHotelDetail(hotel.getName() + " (price TBD — verify before booking)");
            } else {
                breakdown.setHotelDetail(
                        String.format("%s (%s) × %d night(s)",
                                hotel.getName(), plan.isAcRoom() ? "AC" : "Non-AC", nights));
            }
        } else {
            breakdown.setHotelDetail("No hotel selected");
        }
        breakdown.setHotelCost(hotelPrice * nights);

        int localPerPerson = defaults.getLocalTransportPerPerson();
        breakdown.setLocalTransportCost(localPerPerson * travelers);
        breakdown.setLocalTransportDetail(String.format("%d × %d travelers", localPerPerson, travelers));

        int foodPerPerson = defaults.getFoodPerPerson();
        breakdown.setFoodCost(foodPerPerson * travelers);
        breakdown.setFoodDetail(String.format("%d × %d travelers", foodPerPerson, travelers));

        breakdown.setTotal(
                breakdown.getTrainCost()
                        + breakdown.getHotelCost()
                        + breakdown.getLocalTransportCost()
                        + breakdown.getFoodCost());

        return breakdown;
    }

    private int getTransportCost(TransportOption transport) {
        return transport != null ? transport.getCostPerPersonOrZero() : 0;
    }
}
