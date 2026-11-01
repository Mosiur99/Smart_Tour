package com.smarttourplanner.plans;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.planner.PlannerModels.BudgetLevel;
import com.smarttourplanner.planner.PlannerModels.Interest;
import com.smarttourplanner.planner.PlannerModels.PlanRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RequestFingerprintTest {

    @Test
    void ignoresTheOrderOfInterests() {
        Set<Interest> first = new LinkedHashSet<>(List.of(Interest.NATURE, Interest.HERITAGE));
        Set<Interest> second = new LinkedHashSet<>(List.of(Interest.HERITAGE, Interest.NATURE));

        assertThat(RequestFingerprint.of(request(3, first))).isEqualTo(RequestFingerprint.of(request(3, second)));
    }

    @Test
    void changesWhenAnyDetailChanges() {
        assertThat(RequestFingerprint.of(request(3, Set.of()))).isNotEqualTo(RequestFingerprint.of(request(4, Set.of())));
    }

    private static PlanRequest request(int days, Set<Interest> interests) {
        return new PlanRequest("dhaka", "sylhet", days, 2, BudgetLevel.MID, 12, interests);
    }
}
