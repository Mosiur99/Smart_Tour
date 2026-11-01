package com.smarttourplanner.planner;

import static com.smarttourplanner.planner.PlanFixtures.destination;
import static com.smarttourplanner.planner.PlanFixtures.extra;
import static com.smarttourplanner.planner.PlanFixtures.spot;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.planner.PlannerModels.DayPlan;
import com.smarttourplanner.planner.PlannerModels.ScheduledStop;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.StopKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class DaySchedulerTest {

    private static final Stop BEST = spot(1, 1, 22.00, 92.00, 120, 0);
    private static final Stop NEAR = spot(2, 3, 22.01, 92.00, 120, 0);
    private static final Stop FAR = spot(3, 2, 22.30, 92.30, 120, 0);

    @Test
    void startsFromTheBestPlaceThenGoesToTheNearest() {
        List<DayPlan> days = schedule(List.of(BEST, FAR, NEAR), List.of(480));

        assertThat(stopIds(days.get(0))).startsWith(1L, 2L);
    }

    @Test
    void neverExceedsTheDay() {
        List<DayPlan> days = schedule(List.of(BEST, FAR, NEAR), List.of(250));

        assertThat(days.get(0).usedMinutes()).isLessThanOrEqualTo(250);
    }

    @Test
    void countsTravelTimeUsingTheRoadFactor() {
        DayScheduler scheduler = new DayScheduler(destination(List.of(), List.of()));

        // About 1.11 km straight, 1.56 km by road, at 25 km/h.
        assertThat(scheduler.travelMinutes(BEST, NEAR)).isEqualTo(4);
    }

    @Test
    void movesPlacesThatDontFitToTheNextDay() {
        List<DayPlan> days = schedule(List.of(BEST, NEAR), List.of(120, 480));

        assertThat(stopIds(days.get(0))).containsExactly(1L);
        assertThat(stopIds(days.get(1))).containsExactly(2L);
    }

    @Test
    void neverRepeatsAPlace() {
        List<DayPlan> days = schedule(List.of(BEST, NEAR, FAR), List.of(480, 480, 480));

        List<Long> ids = days.stream().flatMap(day -> stopIds(day).stream()).toList();
        assertThat(ids).doesNotHaveDuplicates().hasSize(3);
    }

    @Test
    void leavesADayEmptyWhenNothingFits() {
        List<DayPlan> days = schedule(List.of(BEST), List.of(30, 480));

        assertThat(days.get(0).stops()).isEmpty();
        assertThat(stopIds(days.get(1))).containsExactly(1L);
    }

    @Test
    void givesEachDayItsOwnMealAndPrefersTheNearest() {
        Stop nearMeal = extra(10, StopKind.FOOD, 22.00, 92.00);
        Stop farMeal = extra(11, StopKind.FOOD, 22.50, 92.50);
        Stop unknownPlaceMeal = extra(12, StopKind.FOOD, null, null);
        DayScheduler scheduler = new DayScheduler(
                destination(List.of(BEST, nearMeal, farMeal, unknownPlaceMeal), List.of()));

        List<DayPlan> days = scheduler.schedule(List.of(BEST), List.of(480, 480, 480, 480));

        assertThat(days).extracting(day -> day.meal() == null ? null : day.meal().id())
                .containsExactly(10L, 11L, 12L, null);
    }

    @Test
    void addsAMarketAndATraditionalCraft() {
        Stop market = extra(20, StopKind.MARKET, 22.0, 92.0);
        Stop craft = extra(21, StopKind.CRAFT, 22.0, 92.0);
        DayScheduler scheduler = new DayScheduler(destination(List.of(market, craft), List.of()));

        DayPlan day = scheduler.schedule(List.of(), List.of(480)).get(0);

        assertThat(day.market()).isEqualTo(market);
        assertThat(day.craft()).isEqualTo(craft);
    }

    private List<DayPlan> schedule(List<Stop> stops, List<Integer> minutes) {
        return new DayScheduler(destination(stops, List.of())).schedule(stops, minutes);
    }

    private List<Long> stopIds(DayPlan day) {
        return day.stops().stream().map(ScheduledStop::stop).map(Stop::id).toList();
    }
}
