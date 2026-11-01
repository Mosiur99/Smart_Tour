package com.smarttourplanner.planner;

import com.smarttourplanner.planner.PlannerModels.Interest;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.StopKind;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Puts the places most worth visiting first. Lower priority numbers rank higher. */
final class StopRanker {

    private static final int INTEREST_BONUS = 300;

    private StopRanker() {
    }

    static List<Stop> rank(List<Stop> stops, Set<Interest> interests) {
        return stops.stream()
                .filter(StopRanker::canBeScheduled)
                .sorted(Comparator.comparingInt((Stop stop) -> score(stop, interests)).thenComparingLong(Stop::id))
                .toList();
    }

    private static boolean canBeScheduled(Stop stop) {
        boolean visitable = stop.kind() == StopKind.SPOT || stop.kind() == StopKind.HERITAGE;
        return visitable && stop.lat() != null && stop.lng() != null && stop.visitMinutes() != null;
    }

    private static int score(Stop stop, Set<Interest> interests) {
        return interests.stream().anyMatch(interest -> matches(stop, interest))
                ? stop.priority() - INTEREST_BONUS
                : stop.priority();
    }

    private static boolean matches(Stop stop, Interest interest) {
        return stop.tags().contains(interest.name().toLowerCase(Locale.ROOT))
                || (interest == Interest.HERITAGE && stop.kind() == StopKind.HERITAGE);
    }
}
