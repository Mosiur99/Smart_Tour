package com.smarttourplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.DocumentException;
import com.smarttourplanner.model.BudgetBreakdown;
import com.smarttourplanner.model.BudgetDefaults;
import com.smarttourplanner.model.TourPlan;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PdfExportServiceTest {

    private final PdfExportService service = new PdfExportService();

    @Test
    void generatesAPdfDocument() throws DocumentException {
        byte[] pdf = service.generatePdf(plan());

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void createsAPdfFilename() {
        assertThat(service.generateFilename(plan())).endsWith(".pdf");
    }

    private static TourPlan plan() {
        TourPlan plan = new TourPlan();
        plan.setOriginCity("Dhaka");
        plan.setDistrictId("rajshahi");
        plan.setDistrictName("Rajshahi");
        plan.setTravelers(2);
        plan.setNights(1);
        BudgetBreakdown budget = new BudgetCalculatorService().calculate(plan, new BudgetDefaults());
        plan.setBudget(budget);
        return plan;
    }
}
