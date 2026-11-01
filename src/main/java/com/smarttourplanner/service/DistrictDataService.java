package com.smarttourplanner.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.smarttourplanner.model.District;
import com.smarttourplanner.model.DistrictSummary;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DistrictDataService {

    private static final String INDEX_PATH = "data/districts-index.json";
    private static final String DISTRICT_PATH = "data/districts/%s.json";

    private final ObjectMapper objectMapper;
    private List<DistrictSummary> districtIndex = Collections.emptyList();
    private final Map<String, District> districtCache = new ConcurrentHashMap<>();

    public DistrictDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadIndex() throws IOException {
        ClassPathResource resource = new ClassPathResource(INDEX_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            districtIndex = objectMapper.readValue(inputStream, new TypeReference<List<DistrictSummary>>() {});
        }
    }

    public List<DistrictSummary> getAllDistricts() {
        return districtIndex;
    }

    public List<DistrictSummary> getDistrictsByDivision(String division) {
        return districtIndex.stream()
                .filter(d -> d.getDivision().equalsIgnoreCase(division))
                .collect(Collectors.toList());
    }

    public Optional<DistrictSummary> getDistrictSummary(String id) {
        return districtIndex.stream()
                .filter(d -> d.getId().equals(id))
                .findFirst();
    }

    public Optional<District> getDistrict(String id) {
        District cached = districtCache.get(id);
        if (cached != null) {
            return Optional.of(cached);
        }

        String path = String.format(DISTRICT_PATH, id);
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return Optional.empty();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            District district = objectMapper.readValue(inputStream, District.class);
            districtCache.put(id, district);
            return Optional.of(district);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public List<String> getDivisions() {
        return districtIndex.stream()
                .map(DistrictSummary::getDivision)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
