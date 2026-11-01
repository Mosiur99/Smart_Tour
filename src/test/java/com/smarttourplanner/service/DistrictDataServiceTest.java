package com.smarttourplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttourplanner.model.District;
import com.smarttourplanner.model.DistrictSummary;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DistrictDataServiceTest {

    private DistrictDataService service;

    @BeforeEach
    void loadData() throws IOException {
        ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        service = new DistrictDataService(mapper);
        service.loadIndex();
    }

    @Test
    void indexesAll64Districts() {
        assertThat(service.getAllDistricts()).hasSize(64);
    }

    @Test
    void everyIndexedDistrictHasReadableData() {
        for (DistrictSummary summary : service.getAllDistricts()) {
            assertThat(service.getDistrict(summary.getId()))
                    .as("data for %s", summary.getId())
                    .isPresent();
        }
    }

    @Test
    void returnsTheSameCachedInstanceOnRepeatReads() {
        District first = service.getDistrict("rajshahi").orElseThrow();

        assertThat(service.getDistrict("rajshahi")).containsSame(first);
    }

    @Test
    void filtersByDivisionIgnoringCase() {
        assertThat(service.getDistrictsByDivision("sylhet"))
                .isNotEmpty()
                .allSatisfy(district -> assertThat(district.getDivision()).isEqualTo("Sylhet"));
    }

    @Test
    void listsEightDivisionsInOrder() {
        assertThat(service.getDivisions()).hasSize(8).isSorted();
    }

    @Test
    void returnsEmptyForAnUnknownDistrict() {
        assertThat(service.getDistrict("atlantis")).isEmpty();
    }
}
