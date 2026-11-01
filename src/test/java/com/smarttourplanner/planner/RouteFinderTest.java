package com.smarttourplanner.planner;

import static com.smarttourplanner.planner.PlanFixtures.leg;
import static com.smarttourplanner.planner.PlanFixtures.seasonalLeg;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.planner.PlannerModels.Journey;
import com.smarttourplanner.planner.PlannerModels.Leg;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteFinderTest {

    private final RouteFinder finder = new RouteFinder();

    @Test
    void picksTheFastestDirectLeg() {
        Journey journey = finder.fastest(List.of(leg(1, "a", "b", 300, 500, 500), leg(2, "a", "b", 200, 900, 900)), "a", "b", 1)
                .orElseThrow();

        assertThat(journey.legs()).extracting(Leg::id).containsExactly(2L);
    }

    @Test
    void joinsLegsThroughAHub() {
        Journey journey = finder.fastest(List.of(leg(1, "a", "b", 100, 1, 1), leg(2, "b", "c", 50, 1, 1)), "a", "c", 1)
                .orElseThrow();

        assertThat(journey.totalMinutes()).isEqualTo(150);
        assertThat(journey.legs()).extracting(Leg::id).containsExactly(1L, 2L);
    }

    @Test
    void prefersAFasterConnectionOverASlowDirectLeg() {
        List<Leg> legs = List.of(leg(1, "a", "c", 500, 1, 1), leg(2, "a", "b", 100, 1, 1), leg(3, "b", "c", 100, 1, 1));

        assertThat(finder.fastest(legs, "a", "c", 1).orElseThrow().totalMinutes()).isEqualTo(200);
    }

    @Test
    void acceptsThreeLegs() {
        List<Leg> legs = List.of(leg(1, "a", "b", 10, 1, 1), leg(2, "b", "c", 10, 1, 1), leg(3, "c", "d", 10, 1, 1));

        assertThat(finder.fastest(legs, "a", "d", 1)).isPresent();
    }

    @Test
    void rejectsJourneysOfMoreThanThreeLegs() {
        List<Leg> legs = List.of(
                leg(1, "a", "b", 10, 1, 1), leg(2, "b", "c", 10, 1, 1), leg(3, "c", "d", 10, 1, 1), leg(4, "d", "e", 10, 1, 1));

        assertThat(finder.fastest(legs, "a", "e", 1)).isEmpty();
    }

    @Test
    void ignoresLegsThatDontRunThatMonth() {
        List<Leg> legs = List.of(seasonalLeg(1, "a", "b", 60, Set.of(11, 12, 1)));

        assertThat(finder.fastest(legs, "a", "b", 12)).isPresent();
        assertThat(finder.fastest(legs, "a", "b", 7)).isEmpty();
    }

    @Test
    void returnsAnEmptyJourneyWhenAlreadyThere() {
        assertThat(finder.fastest(List.of(), "a", "a", 1).orElseThrow().legs()).isEmpty();
    }

    @Test
    void isNotConfusedByLoops() {
        List<Leg> legs = List.of(leg(1, "a", "b", 10, 1, 1), leg(2, "b", "a", 10, 1, 1), leg(3, "b", "c", 10, 1, 1));

        assertThat(finder.fastest(legs, "a", "c", 1).orElseThrow().totalMinutes()).isEqualTo(20);
    }

    @Test
    void breaksTiesTheSameWayEveryTime() {
        List<Leg> legs = List.of(leg(2, "a", "b", 100, 1, 1), leg(1, "a", "b", 100, 1, 1));

        assertThat(finder.fastest(legs, "a", "b", 1).orElseThrow().legs().get(0).id()).isEqualTo(1L);
    }
}
