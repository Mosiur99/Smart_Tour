package com.smarttourplanner.service;

import com.smarttourplanner.model.TouristSpot;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteOrderService {

    public List<TouristSpot> orderSpots(List<TouristSpot> spots) {
        return spots.stream()
                .sorted(Comparator.comparingInt(TouristSpot::getPriority))
                .collect(Collectors.toList());
    }
}
