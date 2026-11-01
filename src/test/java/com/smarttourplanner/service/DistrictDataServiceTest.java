package com.smarttourplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttourplanner.model.District;
import com.smarttourplanner.model.DistrictSummary;
import com.smarttourplanner.model.Hotel;
import com.smarttourplanner.model.TouristSpot;
import com.smarttourplanner.model.TransportOption;
import com.smarttourplanner.support.TestDatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestDatabaseConfig.class)
class DistrictDataServiceTest {

    @Autowired
    private DistrictDataService service;

    @Test
    void listsAll64Districts() {
        assertThat(service.getAllDistricts()).hasSize(64);
    }

    @Test
    void everyDistrictHasReadableData() {
        for (DistrictSummary summary : service.getAllDistricts()) {
            assertThat(service.getDistrict(summary.getId())).as("data for %s", summary.getId()).isPresent();
        }
    }

    @Test
    void readsTheSameDistrictRepeatedly() {
        District first = service.getDistrict("rajshahi").orElseThrow();
        District second = service.getDistrict("rajshahi").orElseThrow();

        assertThat(second.getName()).isEqualTo(first.getName());
    }

    @Test
    void filtersByDivisionIgnoringCase() {
        assertThat(service.getDistrictsByDivision("sylhet"))
                .hasSize(4)
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

    @Test
    void rejectsIdsThatPointOutsideTheDistrictFolder() {
        assertThat(service.getDistrict("../districts-index")).isEmpty();
    }

    @Test
    void keepsAcAndNonAcHotelPrices() {
        Hotel hotel = hotel("Hotel Swiss International");

        assertThat(hotel.getAcPrice()).isEqualTo(1800);
        assertThat(hotel.getNonAcPrice()).isEqualTo(1000);
    }

    @Test
    void keepsUnknownHotelPricesEmpty() {
        District district = service.getDistrict("rangamati").orElseThrow();

        assertThat(district.getHotels().get(0).hasPrice()).isFalse();
    }

    @Test
    void keepsTransportFaresDetailsAndDirection() {
        District district = service.getDistrict("rajshahi").orElseThrow();

        TransportOption train = district.getTransport().getFromDhaka().stream()
                .filter(option -> "Banalata Express".equals(option.getName()))
                .findFirst()
                .orElseThrow();
        TransportOption bus = district.getTransport().getFromDhaka().stream()
                .filter(option -> "bus".equals(option.getMode()))
                .findFirst()
                .orElseThrow();

        assertThat(train.getDepartureTime()).isEqualTo("13:30");
        assertThat(train.getCostPerPerson()).isEqualTo(450);
        assertThat(bus.getCostEstimateNote()).isEqualTo("Est. 560-930 BDT (verify before booking)");
        assertThat(district.getTransport().getReturnToDhaka()).isNotEmpty();
    }

    @Test
    void keepsSpotOrderAreaAndFoodLocation() {
        District district = service.getDistrict("rajshahi").orElseThrow();

        assertThat(district.getSpots()).extracting(TouristSpot::getPriority).isSorted();
        assertThat(district.getSpots().get(0).getArea()).isNotBlank();
        assertThat(district.getFoods().get(0).getLocation()).isNotBlank();
    }

    @Test
    void keepsDailyBudgetDefaults() {
        District district = service.getDistrict("rangamati").orElseThrow();

        assertThat(district.getBudgetDefaults().getLocalTransportPerPerson()).isEqualTo(1400);
        assertThat(district.getBudgetDefaults().getFoodPerPerson()).isEqualTo(1200);
    }

    @Test
    void includesUniversitiesAsVisitOptions() {
        District district = service.getDistrict("rajshahi").orElseThrow();

        assertThat(district.getPopularUniversities()).isNotEmpty();
        assertThat(district.getVisitOptions()).anyMatch(option -> "university".equals(option.getArea()));
    }

    private Hotel hotel(String name) {
        return service.getDistrict("rajshahi").orElseThrow().getHotels().stream()
                .filter(hotel -> name.equals(hotel.getName()))
                .findFirst()
                .orElseThrow();
    }
}
