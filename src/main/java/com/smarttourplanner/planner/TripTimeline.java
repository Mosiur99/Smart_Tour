package com.smarttourplanner.planner;

import java.util.ArrayList;
import java.util.List;

/** Works out how many sightseeing minutes each day has after travel. */
final class TripTimeline {

    static final int FULL_DAY_MINUTES = 480;
    static final int OVERNIGHT_FROM_MINUTES = 360;
    static final int MORNING_AFTER_OVERNIGHT_MINUTES = 300;
    static final int DAY_BEFORE_OVERNIGHT_MINUTES = 420;

    private TripTimeline() {
    }

    static List<Integer> availableMinutes(int days, int outboundMinutes, int inboundMinutes) {
        List<Integer> available = new ArrayList<>();
        for (int day = 1; day <= days; day++) {
            int minutes = FULL_DAY_MINUTES;
            if (day == 1) {
                minutes = Math.min(minutes, afterArriving(outboundMinutes));
            }
            if (day == days) {
                minutes = Math.min(minutes, beforeLeaving(inboundMinutes));
            }
            available.add(minutes);
        }
        return available;
    }

    private static int afterArriving(int journeyMinutes) {
        if (journeyMinutes >= OVERNIGHT_FROM_MINUTES) {
            return MORNING_AFTER_OVERNIGHT_MINUTES;
        }
        return FULL_DAY_MINUTES - journeyMinutes;
    }

    private static int beforeLeaving(int journeyMinutes) {
        if (journeyMinutes >= OVERNIGHT_FROM_MINUTES) {
            return DAY_BEFORE_OVERNIGHT_MINUTES;
        }
        return FULL_DAY_MINUTES - journeyMinutes;
    }
}
