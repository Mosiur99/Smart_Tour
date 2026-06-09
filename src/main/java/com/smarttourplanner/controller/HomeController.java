package com.smarttourplanner.controller;

import com.smarttourplanner.model.DistrictSummary;
import com.smarttourplanner.service.DistrictDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final DistrictDataService districtDataService;

    public HomeController(DistrictDataService districtDataService) {
        this.districtDataService = districtDataService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String division,
            Model model) {
        List<DistrictSummary> districts = districtDataService.getAllDistricts();
        if (division != null && !division.isBlank()) {
            districts = districtDataService.getDistrictsByDivision(division);
        }
        model.addAttribute("districts", districts);
        model.addAttribute("divisions", districtDataService.getDivisions());
        model.addAttribute("selectedDivision", division);
        model.addAttribute("originCities", List.of("Dhaka", "Chattogram", "Sylhet", "Khulna", "Rajshahi"));
        return "home";
    }
}
