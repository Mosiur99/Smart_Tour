package com.smarttourplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.model.BudgetBreakdown;
import com.smarttourplanner.model.BudgetDefaults;
import com.smarttourplanner.model.Hotel;
import com.smarttourplanner.model.TourPlan;
import com.smarttourplanner.model.TransportOption;
import org.junit.jupiter.api.Test;

class BudgetCalculatorServiceTest {

    private final BudgetCalculatorService service = new BudgetCalculatorService();

    @Test
    void addsTransportHotelLocalTransportAndFoodForAOneNightTrip() {
        TourPlan plan = plan(2, 1);
        plan.setOutboundTransport(transport(500));
        plan.setReturnTransport(transport(600));
        plan.setSelectedHotel(hotel(1500, 1000));

        BudgetBreakdown budget = service.calculate(plan, defaults(900, 800));

        assertThat(budget.getHotelCost()).isEqualTo(1000);
        assertThat(budget.getLocalTransportCost()).isEqualTo(1800);
        assertThat(budget.getFoodCost()).isEqualTo(1600);
        assertThat(budget.getTotal()).isEqualTo(6600);
    }

    @Test
    void usesTheAcPriceWhenAnAcRoomIsChosen() {
        TourPlan plan = plan(1, 2);
        plan.setAcRoom(true);
        plan.setSelectedHotel(hotel(1500, 1000));

        BudgetBreakdown budget = service.calculate(plan, defaults(0, 0));

        assertThat(budget.getHotelCost()).isEqualTo(3000);
    }

    @Test
    void reportsNoHotelWhenNoneIsSelected() {
        BudgetBreakdown budget = service.calculate(plan(1, 1), defaults(0, 0));

        assertThat(budget.getHotelCost()).isZero();
        assertThat(budget.getHotelDetail()).isEqualTo("No hotel selected");
    }

    @Test
    void treatsZeroTravelersAndNightsAsOne() {
        TourPlan plan = plan(0, 0);
        plan.setOutboundTransport(transport(500));

        BudgetBreakdown budget = service.calculate(plan, defaults(100, 200));

        assertThat(budget.getTotal()).isEqualTo(800);
    }

    @Test
    void listsAllFourCostLines() {
        BudgetBreakdown budget = service.calculate(plan(1, 1), defaults(100, 200));

        assertThat(budget.getLineItems())
                .extracting(BudgetBreakdown.BudgetLineItem::getLabel)
                .containsExactly("Transport (round trip)", "Hotel", "Local transport", "Food");
    }

    private static TourPlan plan(int travelers, int nights) {
        TourPlan plan = new TourPlan();
        plan.setTravelers(travelers);
        plan.setNights(nights);
        return plan;
    }

    private static TransportOption transport(Integer cost) {
        TransportOption option = new TransportOption();
        option.setCostPerPerson(cost);
        return option;
    }

    private static Hotel hotel(Integer acPrice, Integer nonAcPrice) {
        Hotel hotel = new Hotel();
        hotel.setName("Test Hotel");
        hotel.setAcPrice(acPrice);
        hotel.setNonAcPrice(nonAcPrice);
        return hotel;
    }

    private static BudgetDefaults defaults(int localTransport, int food) {
        BudgetDefaults defaults = new BudgetDefaults();
        defaults.setLocalTransportPerPerson(localTransport);
        defaults.setFoodPerPerson(food);
        return defaults;
    }
}
