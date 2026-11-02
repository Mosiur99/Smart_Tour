package com.smarttourplanner.planner;

import static com.smarttourplanner.planner.PlanFixtures.destination;
import static com.smarttourplanner.planner.PlanFixtures.hotel;
import static com.smarttourplanner.planner.PlanFixtures.leg;
import static com.smarttourplanner.planner.PlanFixtures.request;
import static com.smarttourplanner.planner.PlanFixtures.seasonalLeg;
import static com.smarttourplanner.planner.PlanFixtures.spot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttourplanner.planner.PlannerModels.BudgetLevel;
import com.smarttourplanner.planner.PlannerModels.CostCategory;
import com.smarttourplanner.planner.PlannerModels.CostRange;
import com.smarttourplanner.planner.PlannerModels.Hotel;
import com.smarttourplanner.planner.PlannerModels.Leg;
import com.smarttourplanner.planner.PlannerModels.MissingCost;
import com.smarttourplanner.planner.PlannerModels.Notice;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import com.smarttourplanner.planner.PlannerModels.TripPlan;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanEngineTest {

    private static final List<Leg> LEGS = List.of(
            leg(1, "capital", "hill-hub", 300, 500, 700), leg(2, "hill-hub", "capital", 300, 500, 700));
    private static final List<Stop> STOPS = List.of(spot(1, 1, 22.0, 92.0, 60, 100));
    private static final Hotel MID_HOTEL = hotel(1, BudgetLevel.MID, 22.0, 92.0, 2, 2000, 3000);

    private final PlanEngine engine = new PlanEngine();

    @Test
    void givesTheSamePlanForTheSameInput() {
        TripDestination place = destination(STOPS, List.of(MID_HOTEL));

        assertThat(engine.plan(request(3, 2, BudgetLevel.MID), place, LEGS))
                .isEqualTo(engine.plan(request(3, 2, BudgetLevel.MID), place, LEGS));
    }

    @Test
    void addsUpTheBudgetAsARange() {
        TripPlan plan = engine.plan(request(3, 2, BudgetLevel.MID), destination(STOPS, List.of(MID_HOTEL)), LEGS);

        assertThat(cost(plan, CostCategory.TRANSPORT)).isEqualTo(new CostRange(CostCategory.TRANSPORT, 2000, 2800));
        assertThat(cost(plan, CostCategory.HOTEL)).isEqualTo(new CostRange(CostCategory.HOTEL, 4000, 6000));
        assertThat(cost(plan, CostCategory.FOOD)).isEqualTo(new CostRange(CostCategory.FOOD, 4800, 4800));
        assertThat(cost(plan, CostCategory.LOCAL_TRANSPORT)).isEqualTo(new CostRange(CostCategory.LOCAL_TRANSPORT, 3000, 3000));
        assertThat(cost(plan, CostCategory.ENTRY_FEES)).isEqualTo(new CostRange(CostCategory.ENTRY_FEES, 200, 200));
        assertThat(plan.budget().low()).isEqualTo(14000);
        assertThat(plan.budget().high()).isEqualTo(16800);
    }

    @Test
    void booksEnoughRooms() {
        TripPlan plan = engine.plan(request(2, 5, BudgetLevel.MID), destination(STOPS, List.of(MID_HOTEL)), LEGS);

        assertThat(plan.rooms()).isEqualTo(3);
        assertThat(cost(plan, CostCategory.HOTEL).low()).isEqualTo(6000);
    }

    @Test
    void needsNoHotelForADayTrip() {
        TripPlan plan = engine.plan(request(1, 2, BudgetLevel.MID), destination(STOPS, List.of(MID_HOTEL)), LEGS);

        assertThat(plan.nights()).isZero();
        assertThat(plan.hotel()).isNull();
        assertThat(plan.notices()).doesNotContain(Notice.NO_HOTEL_AVAILABLE);
    }

    @Test
    void listsUnknownPricesInsteadOfCountingZero() {
        List<Leg> unknownFares = List.of(leg(1, "capital", "hill-hub", 300, null, null), leg(2, "hill-hub", "capital", 300, 500, 500));
        TripDestination place = destination(List.of(spot(1, 1, 22.0, 92.0, 60, null)), List.of(), PlanFixtures.ALL_YEAR, 1, null, 500);

        TripPlan plan = engine.plan(request(1, 1, BudgetLevel.MID), place, unknownFares);

        assertThat(plan.budget().notIncluded()).containsExactlyInAnyOrder(
                new MissingCost(CostCategory.TRANSPORT, "Service 1", null),
                new MissingCost(CostCategory.FOOD, null, null),
                new MissingCost(CostCategory.ENTRY_FEES, "Spot 1", "স্থান 1"));
    }

    @Test
    void reportsNoRouteOutbound() {
        assertThatThrownBy(() -> engine.plan(request(2, 1, BudgetLevel.MID), destination(STOPS, List.of()), List.of()))
                .isInstanceOf(NoRouteException.class);
    }

    @Test
    void reportsNoRouteBackWhenTheReturnDoesntRunThatMonth() {
        List<Leg> legs = List.of(leg(1, "capital", "hill-hub", 300, 1, 1), seasonalLeg(2, "hill-hub", "capital", 300, Set.of(6)));

        assertThatThrownBy(() -> engine.plan(request(2, 1, BudgetLevel.MID), destination(STOPS, List.of()), legs))
                .isInstanceOf(NoRouteException.class);
    }

    @Test
    void warnsWhenTravellingOffSeason() {
        TripDestination place = destination(STOPS, List.of(MID_HOTEL), Set.of(6), 1, 800, 1000);

        assertThat(engine.plan(request(2, 1, BudgetLevel.MID), place, LEGS).notices()).contains(Notice.OFF_SEASON);
    }

    @Test
    void warnsWhenTheTripIsShorterThanRecommended() {
        TripDestination place = destination(STOPS, List.of(MID_HOTEL), PlanFixtures.ALL_YEAR, 4, 800, 1000);

        assertThat(engine.plan(request(2, 1, BudgetLevel.MID), place, LEGS).notices())
                .contains(Notice.SHORTER_THAN_RECOMMENDED);
    }

    @Test
    void usesTheNearestLevelWhenNoHotelMatchesAndSaysSo() {
        Hotel premium = hotel(1, BudgetLevel.PREMIUM, 22.0, 92.0, 2, 9000, 9000);
        Hotel mid = hotel(2, BudgetLevel.MID, 22.5, 92.5, 2, 3000, 3000);

        TripPlan plan = engine.plan(request(2, 1, BudgetLevel.BUDGET), destination(STOPS, List.of(premium, mid)), LEGS);

        assertThat(plan.hotel()).isEqualTo(mid);
        assertThat(plan.notices()).contains(Notice.HOTEL_AT_OTHER_LEVEL);
    }

    @Test
    void picksTheHotelClosestToTheTripsPlaces() {
        Hotel close = hotel(1, BudgetLevel.MID, 22.0, 92.0, 2, 3000, 3000);
        Hotel far = hotel(2, BudgetLevel.MID, 22.5, 92.5, 2, 1000, 1000);

        assertThat(engine.plan(request(2, 1, BudgetLevel.MID), destination(STOPS, List.of(far, close)), LEGS).hotel())
                .isEqualTo(close);
    }

    @Test
    void warnsWhenNoHotelExists() {
        TripPlan plan = engine.plan(request(2, 1, BudgetLevel.MID), destination(STOPS, List.of()), LEGS);

        assertThat(plan.hotel()).isNull();
        assertThat(plan.notices()).contains(Notice.NO_HOTEL_AVAILABLE);
    }

    private CostRange cost(TripPlan plan, CostCategory category) {
        return plan.budget().costs().stream().filter(cost -> cost.category() == category).findFirst().orElseThrow();
    }
}
