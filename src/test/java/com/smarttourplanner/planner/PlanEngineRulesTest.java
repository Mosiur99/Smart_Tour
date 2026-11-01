package com.smarttourplanner.planner;

import static com.smarttourplanner.planner.PlanFixtures.destination;
import static com.smarttourplanner.planner.PlanFixtures.hotel;
import static com.smarttourplanner.planner.PlanFixtures.leg;
import static com.smarttourplanner.planner.PlanFixtures.spot;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.planner.PlannerModels.BudgetLevel;
import com.smarttourplanner.planner.PlannerModels.CostRange;
import com.smarttourplanner.planner.PlannerModels.DayPlan;
import com.smarttourplanner.planner.PlannerModels.Hotel;
import com.smarttourplanner.planner.PlannerModels.Leg;
import com.smarttourplanner.planner.PlannerModels.PlanRequest;
import com.smarttourplanner.planner.PlannerModels.ScheduledStop;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.TripPlan;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/** Checks rules that must hold for any trip, using random but repeatable inputs. */
class PlanEngineRulesTest {

    private final PlanEngine engine = new PlanEngine();

    @RepeatedTest(300)
    void everyPlanFollowsTheRules(RepetitionInfo repetition) {
        Random random = new Random(repetition.getCurrentRepetition());
        int days = 1 + random.nextInt(14);
        int people = 1 + random.nextInt(30);
        int travelMinutes = 30 + random.nextInt(900);
        List<Leg> legs = List.of(
                leg(1, "capital", "hill-hub", travelMinutes, 100, 500), leg(2, "hill-hub", "capital", travelMinutes, 100, 500));
        List<Hotel> hotels = List.of(hotel(1, BudgetLevel.values()[random.nextInt(3)], 22.0, 92.0, 1 + random.nextInt(4), 1000, 3000));
        PlanRequest request = new PlanRequest(
                "capital", "hill-town", days, people, BudgetLevel.values()[random.nextInt(3)], 1 + random.nextInt(12), Set.of());

        TripPlan plan = engine.plan(request, destination(randomStops(random), hotels), legs);

        assertThat(plan.dayPlans()).hasSize(days);
        assertThat(plan.dayPlans()).allSatisfy(day -> {
            assertThat(day.usedMinutes()).isLessThanOrEqualTo(day.availableMinutes());
            assertThat(day.availableMinutes()).isLessThanOrEqualTo(TripTimeline.FULL_DAY_MINUTES);
        });
        assertThat(visitedIds(plan)).doesNotHaveDuplicates();
        assertThat(plan.budget().low()).isEqualTo(plan.budget().costs().stream().mapToInt(CostRange::low).sum());
        assertThat(plan.budget().high()).isEqualTo(plan.budget().costs().stream().mapToInt(CostRange::high).sum());
        assertThat(plan.budget().low()).isPositive().isLessThanOrEqualTo(plan.budget().high());
        if (plan.hotel() != null) {
            assertThat(plan.rooms() * plan.hotel().roomCapacity()).isGreaterThanOrEqualTo(people);
        }
    }

    private List<Stop> randomStops(Random random) {
        List<Stop> stops = new ArrayList<>();
        int count = random.nextInt(40);
        for (int id = 1; id <= count; id++) {
            stops.add(spot(id, random.nextInt(200), 21 + random.nextDouble(), 91 + random.nextDouble(),
                    15 + random.nextInt(300), random.nextBoolean() ? 50 : null));
        }
        return stops;
    }

    private List<Long> visitedIds(TripPlan plan) {
        List<Long> ids = new ArrayList<>();
        for (DayPlan day : plan.dayPlans()) {
            for (ScheduledStop item : day.stops()) {
                ids.add(item.stop().id());
            }
        }
        return ids;
    }
}
