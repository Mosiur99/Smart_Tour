package com.smarttourplanner.service;

import com.smarttourplanner.catalog.CatalogModels;
import com.smarttourplanner.catalog.CatalogModels.DestinationDetail;
import com.smarttourplanner.catalog.CatalogModels.DestinationSummary;
import com.smarttourplanner.catalog.CatalogModels.Place;
import com.smarttourplanner.model.BudgetDefaults;
import com.smarttourplanner.model.District;
import com.smarttourplanner.model.DistrictSummary;
import com.smarttourplanner.model.DistrictTransport;
import com.smarttourplanner.model.FoodPlace;
import com.smarttourplanner.model.Hotel;
import com.smarttourplanner.model.TouristSpot;
import com.smarttourplanner.model.TransportOption;
import com.smarttourplanner.model.University;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Converts catalog data into the models the JSP pages use. Step 7 replaces those pages. */
final class DistrictMapper {

    private static final String DHAKA_HUB = "dhaka";
    private static final String CHECK_BEFORE_BOOKING = "check_live_before_booking";

    private DistrictMapper() {
    }

    static DistrictSummary toSummary(DestinationSummary destination) {
        DistrictSummary summary = new DistrictSummary();
        summary.setId(destination.slug());
        summary.setName(destination.nameEn());
        summary.setDivision(destination.divisionNameEn());
        summary.setHasDetailData(true);
        return summary;
    }

    static District toDistrict(DestinationDetail detail) {
        District district = new District();
        district.setId(detail.summary().slug());
        district.setName(detail.summary().nameEn());
        district.setDivision(detail.summary().divisionNameEn());
        district.setTourGuideUrl(detail.guideUrl());
        district.setHotelNote(detail.hotelNote());
        district.setTravelNote(detail.travelNote());
        district.setUniversityNote(detail.universityNote());
        district.setBudgetDefaults(toBudgetDefaults(detail));
        district.setTransport(toTransport(detail.transport()));
        district.setSpots(detail.places().spots().stream().map(DistrictMapper::toSpot).toList());
        district.setHistoricalPlaces(detail.places().heritage().stream().map(DistrictMapper::toSpot).toList());
        district.setFoods(detail.places().food().stream().map(DistrictMapper::toFood).toList());
        district.setHotels(detail.hotels().stream().map(DistrictMapper::toHotel).toList());
        district.setPopularUniversities(detail.universities().stream().map(DistrictMapper::toUniversity).toList());
        return district;
    }

    private static BudgetDefaults toBudgetDefaults(DestinationDetail detail) {
        BudgetDefaults defaults = new BudgetDefaults();
        if (detail.localTransportDaily() != null) {
            defaults.setLocalTransportPerPerson(detail.localTransportDaily());
        }
        if (detail.foodDaily() != null) {
            defaults.setFoodPerPerson(detail.foodDaily());
        }
        return defaults;
    }

    private static DistrictTransport toTransport(CatalogModels.Transport transport) {
        DistrictTransport result = new DistrictTransport();
        result.setFromDhaka(toOptions(transport.arriving(), leg -> DHAKA_HUB.equals(leg.fromHub())));
        result.setReturnToDhaka(toOptions(transport.leaving(), leg -> DHAKA_HUB.equals(leg.toHub())));
        return result;
    }

    private static List<TransportOption> toOptions(
            List<CatalogModels.TransportOption> legs, Predicate<CatalogModels.TransportOption> filter) {
        return legs.stream().filter(filter).map(DistrictMapper::toOption).toList();
    }

    private static TransportOption toOption(CatalogModels.TransportOption leg) {
        TransportOption option = new TransportOption();
        option.setMode(leg.mode().toLowerCase(Locale.ROOT));
        option.setName(leg.serviceName());
        option.setDepartureTime(leg.departureNote());
        option.setCostPerPerson(leg.fareMin());
        option.setNote(leg.note());
        option.setCostVerificationStatus(CHECK_BEFORE_BOOKING);
        if (leg.fareMin() != null && !leg.fareMin().equals(leg.fareMax())) {
            option.setCostEstimateNote(
                    "Est. %d-%d BDT (verify before booking)".formatted(leg.fareMin(), leg.fareMax()));
        }
        return option;
    }

    private static TouristSpot toSpot(Place place) {
        TouristSpot spot = new TouristSpot();
        spot.setName(place.nameEn());
        spot.setPriority(place.priority());
        spot.setTip(place.note());
        if (!place.tags().isEmpty()) {
            spot.setArea(place.tags().get(0));
        }
        return spot;
    }

    private static FoodPlace toFood(Place place) {
        FoodPlace food = new FoodPlace();
        food.setName(place.nameEn());
        food.setLocation(place.note());
        return food;
    }

    private static Hotel toHotel(CatalogModels.Hotel source) {
        Hotel hotel = new Hotel();
        hotel.setName(source.nameEn());
        hotel.setLocation(source.address());
        hotel.setReview(source.note());
        hotel.setWebsite(source.website());
        hotel.setNonAcPrice(source.priceMin());
        hotel.setAcPrice(source.priceMax());
        return hotel;
    }

    private static University toUniversity(CatalogModels.University source) {
        University university = new University();
        university.setName(source.nameEn());
        university.setType(source.kind());
        university.setLocation(source.location());
        university.setNote(source.note());
        return university;
    }
}
