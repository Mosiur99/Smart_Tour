package com.smarttourplanner.planner;

import static com.smarttourplanner.planner.PlanFixtures.extra;
import static com.smarttourplanner.planner.PlanFixtures.spot;
import static com.smarttourplanner.planner.PlanFixtures.tagged;
import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.planner.PlannerModels.Interest;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.StopKind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StopRankerTest {

    @Test
    void ranksByPriority() {
        List<Stop> ranked = StopRanker.rank(List.of(tagged(1, 20, "x"), tagged(2, 10, "x")), Set.of());

        assertThat(ranked).extracting(Stop::id).containsExactly(2L, 1L);
    }

    @Test
    void movesMatchingInterestsUp() {
        List<Stop> ranked = StopRanker.rank(
                List.of(tagged(1, 10, "city"), tagged(2, 50, "nature")), Set.of(Interest.NATURE));

        assertThat(ranked).extracting(Stop::id).containsExactly(2L, 1L);
    }

    @Test
    void treatsHeritageSitesAsMatchingTheHeritageInterest() {
        Stop site = new Stop(9, StopKind.HERITAGE, "Fort", null, 22.0, 92.0, 50, 60, 0, 0, Set.of());

        List<Stop> ranked = StopRanker.rank(List.of(tagged(1, 10, "city"), site), Set.of(Interest.HERITAGE));

        assertThat(ranked).extracting(Stop::id).containsExactly(9L, 1L);
    }

    @Test
    void leavesOutPlacesThatCantBeRouted() {
        Stop noCoordinates = new Stop(1, StopKind.SPOT, "A", null, null, null, 1, 60, 0, 0, Set.of());
        Stop noVisitTime = new Stop(2, StopKind.SPOT, "B", null, 22.0, 92.0, 1, null, 0, 0, Set.of());

        assertThat(StopRanker.rank(
                List.of(noCoordinates, noVisitTime, extra(3, StopKind.FOOD, 22.0, 92.0), spot(4, 1, 22.0, 92.0, 60, 0)),
                Set.of()))
                .extracting(Stop::id)
                .containsExactly(4L);
    }
}
