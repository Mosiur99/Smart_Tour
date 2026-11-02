package com.smarttourplanner.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Response shapes for the public catalog API. A null price means unknown. */
public final class CatalogModels {

    private CatalogModels() {
    }

    public record Hub(String slug, String nameEn, @Nullable String nameBn) {
    }

    public record DestinationSummary(
            String slug, String nameEn, @Nullable String nameBn, String divisionSlug, String divisionNameEn,
            String divisionNameBn, boolean verified) {
    }

    public record DestinationPage(List<DestinationSummary> items, @Nullable String nextCursor) {
    }

    public record Place(
            String nameEn, @Nullable String nameBn, @Nullable Double lat, @Nullable Double lng, int priority,
            @Nullable Integer visitMinutes, @Nullable Integer feeMin, @Nullable Integer feeMax, List<String> tags,
            @Nullable String note, @Nullable LocalDate checkedAt, boolean verified) {
    }

    // The planner has its own Hotel, so this one needs a distinct name in the API contract.
    @Schema(name = "DestinationHotel")
    public record Hotel(
            String nameEn, @Nullable String nameBn, @Nullable String tier, @Nullable String address,
            Integer roomCapacity, @Nullable Integer priceMin, @Nullable Integer priceMax, @Nullable String website,
            @Nullable String note, @Nullable LocalDate checkedAt, boolean verified) {
    }

    public record TransportOption(
            String fromHub, String toHub, String mode, String serviceName, @Nullable Integer durationMinutes,
            @Nullable String departureNote, @Nullable Integer fareMin, @Nullable Integer fareMax, @Nullable String note,
            @Nullable LocalDate checkedAt, boolean verified) {
    }

    public record University(String nameEn, @Nullable String kind, @Nullable String location, @Nullable String note) {
    }

    public record Places(
            List<Place> spots, List<Place> heritage, List<Place> markets, List<Place> food, List<Place> crafts) {
    }

    public record Transport(List<TransportOption> arriving, List<TransportOption> leaving) {
    }

    public record DestinationDetail(
            DestinationSummary summary, @Nullable Double lat, @Nullable Double lng,
            @Nullable Integer localTransportDaily, @Nullable Integer foodDaily, @Nullable String travelNote,
            @Nullable String hotelNote, @Nullable String universityNote, @Nullable String guideUrl, Places places,
            List<Hotel> hotels, List<University> universities, Transport transport) {
    }
}
