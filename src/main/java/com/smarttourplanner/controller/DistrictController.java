package com.smarttourplanner.controller;

import com.smarttourplanner.model.District;
import com.smarttourplanner.service.DistrictDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DistrictController {

    private final DistrictDataService districtDataService;

    public DistrictController(DistrictDataService districtDataService) {
        this.districtDataService = districtDataService;
    }

    @GetMapping("/district/{id}")
    public String districtDetail(@PathVariable String id, Model model) {
        District district = districtDataService.getDistrict(id)
                .orElseThrow(() -> new DistrictNotFoundException(id));

        model.addAttribute("district", district);
        model.addAttribute("hasDetailData", districtDataService.getDistrictSummary(id)
                .map(summary -> summary.isHasDetailData())
                .orElse(false));
        return "district-detail";
    }
}
