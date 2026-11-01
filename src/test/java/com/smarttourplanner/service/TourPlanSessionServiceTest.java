package com.smarttourplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.model.TourPlan;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class TourPlanSessionServiceTest {

    private final TourPlanSessionService service = new TourPlanSessionService();
    private final MockHttpSession session = new MockHttpSession();

    @Test
    void returnsNullWhenNoPlanIsStored() {
        assertThat(service.getPlan(session)).isNull();
    }

    @Test
    void storesAndReturnsThePlan() {
        TourPlan plan = new TourPlan();

        service.savePlan(session, plan);

        assertThat(service.getPlan(session)).isSameAs(plan);
    }

    @Test
    void clearsThePlan() {
        service.savePlan(session, new TourPlan());

        service.clearPlan(session);

        assertThat(service.getPlan(session)).isNull();
    }
}
