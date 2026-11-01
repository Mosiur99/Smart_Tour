package com.smarttourplanner.catalog;

import java.time.LocalDate;
import java.util.List;

/** Response shapes for the public catalog API. A null price means unknown. */
public final class CatalogModels {

    private CatalogModels() {
    }

    public record Hub(String slug, String nameEn, String nameBn) {
    }

    public record DestinationSummary(
            String slug, String nameEn, String nameBn, String divisionSlug, String divisionNameEn,
            String divisionNameBn, boolean verified) {
    }

    public record DestinationPage(List<DestinationSummary> items, String nextCursor) {
    }

    public record Place(
            String nameEn, String nameBn, Double lat, Double lng, int priority, Integer visitMinutes,
            Integer feeMin, Integer feeMax, List<String> tags, String note, LocalDate checkedAt, boolean verified) {
    }

    public record Hotel(
            String nameEn, String nameBn, String tier, String address, Integer roomCapacity, Integer priceMin,
            Integer priceMax, String website, String note, LocalDate checkedAt, boolean verified) {
    }

    public record TransportOption(
            String fromHub, String toHub, String mode, String serviceName, Integer durationMinutes,
            String departureNote, Integer fareMin, Integer fareMax, String note, LocalDate checkedAt,
            boolean verified) {
    }

    public record University(String nameEn, String kind, String location, String note) {
    }

    public record Places(
            List<Place> spots, List<Place> heritage, List<Place> markets, List<Place> food, List<Place> crafts) {
    }

    public record Transport(List<TransportOption> arriving, List<TransportOption> leaving) {
    }

    public record DestinationDetail(
            DestinationSummary summary, Double lat, Double lng, Integer localTransportDaily, Integer foodDaily,
            String travelNote, String hotelNote, String universityNote, String guideUrl, Places places,
            List<Hotel> hotels, List<University> universities, Transport transport) {
    }
}
