package com.smarttourplanner.planner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TripTimelineTest {

    @Test
    void shortJourneysTakeTimeFromTheFirstAndLastDay() {
        assertThat(TripTimeline.availableMinutes(3, 120, 180)).containsExactly(360, 480, 300);
    }

    @Test
    void longJourneysRunOvernight() {
        assertThat(TripTimeline.availableMinutes(3, 600, 600)).containsExactly(300, 480, 420);
    }

    @Test
    void aOneDayTripCountsBothJourneys() {
        assertThat(TripTimeline.availableMinutes(1, 120, 180)).containsExactly(300);
    }

    @Test
    void anOvernightJourneyStartsAtTheThreshold() {
        assertThat(TripTimeline.availableMinutes(2, 359, 480).get(0)).isEqualTo(121);
        assertThat(TripTimeline.availableMinutes(2, 360, 480).get(0)).isEqualTo(300);
    }
}
