package com.smarttourplanner.catalog;

import com.smarttourplanner.catalog.CatalogModels.DestinationDetail;
import com.smarttourplanner.catalog.CatalogModels.DestinationPage;
import com.smarttourplanner.catalog.CatalogModels.DestinationSummary;
import com.smarttourplanner.catalog.CatalogModels.Hub;
import com.smarttourplanner.catalog.CatalogModels.Places;
import com.smarttourplanner.catalog.CatalogModels.Transport;
import com.smarttourplanner.catalog.CatalogModels.TransportOption;
import com.smarttourplanner.catalog.CatalogRepository.DestinationRow;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private static final int ALL_DESTINATIONS = 1_000;

    private final CatalogRepository repository;
    private final VersionedCache cache;

    public CatalogService(CatalogRepository repository, VersionedCache cache) {
        this.repository = repository;
        this.cache = cache;
    }

    public List<Hub> hubs() {
        return cache.get("hubs", repository::findHubs);
    }

    public DestinationPage destinations(String query, String division, String cursor, int limit) {
        if (query != null && !query.isBlank()) {
            return search(query.strip(), division, limit);
        }
        return cache.get("page:%s:%s:%d".formatted(division, cursor, limit), () -> page(division, cursor, limit));
    }

    public List<DestinationSummary> allDestinations() {
        return repository.findDestinations(null, null, ALL_DESTINATIONS);
    }

    public Optional<DestinationDetail> destination(String slug) {
        return Optional.ofNullable(cache.get("destination:" + slug, () -> loadDetail(slug)));
    }

    private DestinationPage search(String query, String division, int limit) {
        String key = "search:%s:%s:%d".formatted(query.toLowerCase(Locale.ROOT), division, limit);
        return cache.get(key, () -> new DestinationPage(repository.searchDestinations(query, division, limit), null));
    }

    private DestinationPage page(String division, String cursor, int limit) {
        List<DestinationSummary> rows = repository.findDestinations(division, cursor, limit);
        if (rows.size() <= limit) {
            return new DestinationPage(rows, null);
        }
        List<DestinationSummary> items = rows.subList(0, limit);
        return new DestinationPage(List.copyOf(items), items.get(limit - 1).slug());
    }

    private DestinationDetail loadDetail(String slug) {
        return repository.findDestination(slug).map(this::toDetail).orElse(null);
    }

    private DestinationDetail toDetail(DestinationRow row) {
        return new DestinationDetail(
                row.summary(), row.lat(), row.lng(), row.localTransportDaily(), row.foodDaily(),
                row.travelNote(), row.hotelNote(), row.universityNote(), row.guideUrl(),
                places(row.id()), repository.findHotels(row.id()), repository.findUniversities(row.id()),
                transport(row.arrivalHubId()));
    }

    private Places places(long destinationId) {
        return new Places(
                repository.findPlaces(destinationId, "SPOT"),
                repository.findPlaces(destinationId, "HERITAGE"),
                repository.findPlaces(destinationId, "MARKET"),
                repository.findPlaces(destinationId, "FOOD"),
                repository.findPlaces(destinationId, "CRAFT"));
    }

    private Transport transport(Long hubId) {
        if (hubId == null) {
            List<TransportOption> none = List.of();
            return new Transport(none, none);
        }
        return new Transport(repository.findLegsArrivingAt(hubId), repository.findLegsLeaving(hubId));
    }
}
