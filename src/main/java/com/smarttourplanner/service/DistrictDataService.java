package com.smarttourplanner.service;

import com.smarttourplanner.catalog.CatalogService;
import com.smarttourplanner.catalog.VersionedCache;
import com.smarttourplanner.model.District;
import com.smarttourplanner.model.DistrictSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Gives the JSP pages their data from the catalog database. */
@Service
public class DistrictDataService {

    private final CatalogService catalog;
    private final VersionedCache cache;

    public DistrictDataService(CatalogService catalog, VersionedCache cache) {
        this.catalog = catalog;
        this.cache = cache;
    }

    public List<DistrictSummary> getAllDistricts() {
        return cache.get("pages:districts", this::loadDistricts);
    }

    public List<DistrictSummary> getDistrictsByDivision(String division) {
        return getAllDistricts().stream()
                .filter(district -> district.getDivision().equalsIgnoreCase(division))
                .toList();
    }

    public Optional<DistrictSummary> getDistrictSummary(String id) {
        return getAllDistricts().stream()
                .filter(district -> district.getId().equals(id))
                .findFirst();
    }

    public Optional<District> getDistrict(String id) {
        return catalog.destination(id).map(DistrictMapper::toDistrict);
    }

    public List<String> getDivisions() {
        return getAllDistricts().stream()
                .map(DistrictSummary::getDivision)
                .distinct()
                .sorted()
                .toList();
    }

    private List<DistrictSummary> loadDistricts() {
        return catalog.allDestinations().stream()
                .map(DistrictMapper::toSummary)
                .sorted(Comparator.comparing(DistrictSummary::getDivision).thenComparing(DistrictSummary::getName))
                .toList();
    }
}
