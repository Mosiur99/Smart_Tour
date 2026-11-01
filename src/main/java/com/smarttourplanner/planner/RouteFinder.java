package com.smarttourplanner.planner;

import com.smarttourplanner.planner.PlannerModels.Journey;
import com.smarttourplanner.planner.PlannerModels.Leg;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Finds the fastest journey between two hubs using legs that run in the travel month. */
final class RouteFinder {

    static final int MAX_LEGS = 3;

    private static final Comparator<Leg> STABLE_ORDER =
            Comparator.comparingInt(Leg::durationMinutes).thenComparingLong(Leg::id);

    Optional<Journey> fastest(List<Leg> legs, String fromHub, String toHub, int month) {
        List<Leg> running = legs.stream()
                .filter(leg -> leg.months().contains(month))
                .sorted(STABLE_ORDER)
                .toList();

        Map<String, Journey> best = new HashMap<>();
        best.put(fromHub, new Journey(List.of(), 0));
        for (int round = 0; round < MAX_LEGS; round++) {
            best = extendByOneLeg(best, running);
        }
        return Optional.ofNullable(best.get(toHub));
    }

    private Map<String, Journey> extendByOneLeg(Map<String, Journey> reached, List<Leg> legs) {
        Map<String, Journey> extended = new HashMap<>(reached);
        for (Leg leg : legs) {
            Journey start = reached.get(leg.fromHub());
            if (start != null) {
                Journey candidate = append(start, leg);
                Journey current = extended.get(leg.toHub());
                if (current == null || candidate.totalMinutes() < current.totalMinutes()) {
                    extended.put(leg.toHub(), candidate);
                }
            }
        }
        return extended;
    }

    private Journey append(Journey journey, Leg leg) {
        List<Leg> legs = new ArrayList<>(journey.legs());
        legs.add(leg);
        return new Journey(List.copyOf(legs), journey.totalMinutes() + leg.durationMinutes());
    }
}
