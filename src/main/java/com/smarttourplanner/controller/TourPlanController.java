package com.smarttourplanner.controller;

import com.lowagie.text.DocumentException;
import com.smarttourplanner.model.District;
import com.smarttourplanner.model.Hotel;
import com.smarttourplanner.model.TourPlan;
import com.smarttourplanner.model.TouristSpot;
import com.smarttourplanner.model.TransportOption;
import com.smarttourplanner.service.BudgetCalculatorService;
import com.smarttourplanner.service.DistrictDataService;
import com.smarttourplanner.service.PdfExportService;
import com.smarttourplanner.service.RouteOrderService;
import com.smarttourplanner.service.TourPlanSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/plan")
public class TourPlanController {

    private final DistrictDataService districtDataService;
    private final BudgetCalculatorService budgetCalculatorService;
    private final RouteOrderService routeOrderService;
    private final TourPlanSessionService sessionService;
    private final PdfExportService pdfExportService;

    public TourPlanController(
            DistrictDataService districtDataService,
            BudgetCalculatorService budgetCalculatorService,
            RouteOrderService routeOrderService,
            TourPlanSessionService sessionService,
            PdfExportService pdfExportService) {
        this.districtDataService = districtDataService;
        this.budgetCalculatorService = budgetCalculatorService;
        this.routeOrderService = routeOrderService;
        this.sessionService = sessionService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/build/{districtId}")
    public String buildForm(
            @PathVariable String districtId,
            @RequestParam(defaultValue = "Dhaka") String origin,
            Model model) {
        District district = districtDataService.getDistrict(districtId)
                .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtId));

        model.addAttribute("district", district);
        model.addAttribute("visitOptions", district.getVisitOptions());
        model.addAttribute("originCity", origin);
        return "tour-builder";
    }

    @PostMapping("/save")
    public String savePlan(
            @RequestParam String districtId,
            @RequestParam String originCity,
            @RequestParam(defaultValue = "0") int outboundIndex,
            @RequestParam(defaultValue = "0") int returnIndex,
            @RequestParam(required = false) List<String> spotNames,
            @RequestParam(defaultValue = "0") int hotelIndex,
            @RequestParam(defaultValue = "false") boolean acRoom,
            @RequestParam(defaultValue = "1") int travelers,
            @RequestParam(defaultValue = "1") int nights,
            HttpSession session) {

        District district = districtDataService.getDistrict(districtId)
                .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtId));

        TourPlan plan = new TourPlan();
        plan.setOriginCity(originCity);
        plan.setDistrictId(districtId);
        plan.setDistrictName(district.getName());
        plan.setTravelers(Math.max(travelers, 1));
        plan.setNights(Math.max(nights, 1));
        plan.setAcRoom(acRoom);
        plan.setTourGuideUrl(district.getTourGuideUrl());
        plan.setFoods(district.getFoods());

        List<TransportOption> outbound = district.getTransport().getFromDhaka();
        if (!outbound.isEmpty() && outboundIndex >= 0 && outboundIndex < outbound.size()) {
            plan.setOutboundTransport(outbound.get(outboundIndex));
        }

        List<TransportOption> returnOptions = district.getTransport().getReturnToDhaka();
        if (!returnOptions.isEmpty() && returnIndex >= 0 && returnIndex < returnOptions.size()) {
            plan.setReturnTransport(returnOptions.get(returnIndex));
        }

        List<TouristSpot> selectedSpots = filterSelectedSpots(district.getVisitOptions(), spotNames);
        plan.setSelectedSpots(routeOrderService.orderSpots(selectedSpots));

        List<Hotel> hotels = district.getHotels();
        if (hotelIndex >= 0 && !hotels.isEmpty() && hotelIndex < hotels.size()) {
            plan.setSelectedHotel(hotels.get(hotelIndex));
        }

        plan.setBudget(budgetCalculatorService.calculate(plan, district.getBudgetDefaults()));
        sessionService.savePlan(session, plan);

        return "redirect:/plan/summary";
    }

    @GetMapping("/summary")
    public String summary(HttpSession session, Model model) {
        TourPlan plan = sessionService.getPlan(session);
        if (plan == null) {
            return "redirect:/";
        }
        model.addAttribute("plan", plan);
        return "plan-summary";
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(HttpSession session) throws DocumentException {
        TourPlan plan = sessionService.getPlan(session);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdf = pdfExportService.generatePdf(plan);
        String filename = pdfExportService.generateFilename(plan);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/new")
    public String newPlan(HttpSession session) {
        sessionService.clearPlan(session);
        return "redirect:/";
    }

    private List<TouristSpot> filterSelectedSpots(List<TouristSpot> allSpots, List<String> spotNames) {
        if (spotNames == null || spotNames.isEmpty()) {
            return new ArrayList<>();
        }
        return allSpots.stream()
                .filter(spot -> spotNames.contains(spot.getName()))
                .collect(Collectors.toList());
    }
}
