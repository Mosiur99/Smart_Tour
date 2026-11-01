package com.smarttourplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.model.TouristSpot;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteOrderServiceTest {

    private final RouteOrderService service = new RouteOrderService();

    @Test
    void ordersSpotsByPriority() {
        List<TouristSpot> ordered = service.orderSpots(List.of(spot("Third", 3), spot("First", 1), spot("Second", 2)));

        assertThat(ordered).extracting(TouristSpot::getName).containsExactly("First", "Second", "Third");
    }

    @Test
    void returnsAnEmptyListForNoSpots() {
        assertThat(service.orderSpots(List.of())).isEmpty();
    }

    private static TouristSpot spot(String name, int priority) {
        TouristSpot spot = new TouristSpot();
        spot.setName(name);
        spot.setPriority(priority);
        return spot;
    }
}
