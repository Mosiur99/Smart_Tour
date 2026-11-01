package com.smarttourplanner.planner;

import com.smarttourplanner.planner.PlannerModels.DayPlan;
import com.smarttourplanner.planner.PlannerModels.ScheduledStop;
import com.smarttourplanner.planner.PlannerModels.Stop;
import com.smarttourplanner.planner.PlannerModels.StopKind;
import com.smarttourplanner.planner.PlannerModels.TripDestination;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Fills each day from the best remaining place, then keeps adding the nearest place that fits. */
final class DayScheduler {

    private static final double LOCAL_SPEED_KMH = 25.0;

    private final TripDestination destination;

    DayScheduler(TripDestination destination) {
        this.destination = destination;
    }

    List<DayPlan> schedule(List<Stop> rankedStops, List<Integer> dayMinutes) {
        List<Stop> remaining = new ArrayList<>(rankedStops);
        List<Stop> meals = extras(StopKind.FOOD);
        List<Stop> markets = extras(StopKind.MARKET);
        List<Stop> crafts = extras(StopKind.CRAFT);

        List<DayPlan> days = new ArrayList<>();
        for (int index = 0; index < dayMinutes.size(); index++) {
            int available = dayMinutes.get(index);
            List<ScheduledStop> stops = fillDay(remaining, available);
            days.add(new DayPlan(
                    index + 1, available, usedMinutes(stops), stops,
                    takeNearest(meals, stops), takeNearest(markets, stops), takeNearest(crafts, stops)));
        }
        return days;
    }

    private List<ScheduledStop> fillDay(List<Stop> remaining, int available) {
        List<ScheduledStop> stops = new ArrayList<>();
        int left = available;
        Optional<Stop> next = remaining.stream().filter(stop -> stop.visitMinutes() <= available).findFirst();
        int travel = 0;
        while (next.isPresent()) {
            Stop stop = next.get();
            stops.add(new ScheduledStop(stop, travel));
            remaining.remove(stop);
            left -= travel + stop.visitMinutes();
            next = nearestThatFits(remaining, stop, left);
            travel = next.map(found -> travelMinutes(stop, found)).orElse(0);
        }
        return List.copyOf(stops);
    }

    private Optional<Stop> nearestThatFits(List<Stop> remaining, Stop from, int minutesLeft) {
        return remaining.stream()
                .filter(stop -> travelMinutes(from, stop) + stop.visitMinutes() <= minutesLeft)
                .min(Comparator.comparingInt((Stop stop) -> travelMinutes(from, stop)).thenComparingLong(Stop::id));
    }

    int travelMinutes(Stop from, Stop to) {
        double roadKm = GeoDistance.kilometers(from.lat(), from.lng(), to.lat(), to.lng()) * destination.roadFactor();
        return (int) Math.ceil(roadKm / LOCAL_SPEED_KMH * 60);
    }

    private int usedMinutes(List<ScheduledStop> stops) {
        return stops.stream().mapToInt(item -> item.travelMinutes() + item.stop().visitMinutes()).sum();
    }

    private List<Stop> extras(StopKind kind) {
        return new ArrayList<>(destination.stops().stream()
                .filter(stop -> stop.kind() == kind)
                .sorted(Comparator.comparingInt(Stop::priority).thenComparingLong(Stop::id))
                .toList());
    }

    /** Places without coordinates come last, in priority order. Each extra is used once. */
    private Stop takeNearest(List<Stop> extras, List<ScheduledStop> dayStops) {
        double[] center = center(dayStops);
        Optional<Stop> nearest = extras.stream().min(Comparator.comparingDouble(extra -> distance(extra, center)));
        nearest.ifPresent(extras::remove);
        return nearest.orElse(null);
    }

    private double[] center(List<ScheduledStop> dayStops) {
        if (dayStops.isEmpty()) {
            return new double[] {destination.lat(), destination.lng()};
        }
        double lat = dayStops.stream().mapToDouble(item -> item.stop().lat()).average().orElseThrow();
        double lng = dayStops.stream().mapToDouble(item -> item.stop().lng()).average().orElseThrow();
        return new double[] {lat, lng};
    }

    private double distance(Stop extra, double[] center) {
        if (extra.lat() == null || extra.lng() == null) {
            return Double.MAX_VALUE;
        }
        return GeoDistance.kilometers(extra.lat(), extra.lng(), center[0], center[1]);
    }
}
