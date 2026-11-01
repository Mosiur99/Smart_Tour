package com.smarttourplanner.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smarttourplanner.model.BudgetBreakdown;
import com.smarttourplanner.model.FoodPlace;
import com.smarttourplanner.model.Hotel;
import com.smarttourplanner.model.TourPlan;
import com.smarttourplanner.model.TouristSpot;
import com.smarttourplanner.model.TransportOption;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PdfExportService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font HEADING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    public byte[] generatePdf(TourPlan plan) throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        addTitle(document, plan);
        addTransportSection(document, plan);
        addSpotsSection(document, plan);
        addFoodSection(document, plan);
        addHotelSection(document, plan);
        addBudgetSection(document, plan);
        addGuideSection(document, plan);

        document.close();
        return outputStream.toByteArray();
    }

    public String generateFilename(TourPlan plan) {
        String district = plan.getDistrictName() != null
                ? plan.getDistrictName().replaceAll("\\s+", "-")
                : "tour";
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        return "Tour-Plan-" + district + "-" + date + ".pdf";
    }

    private void addTitle(Document document, TourPlan plan) throws DocumentException {
        Paragraph title = new Paragraph(
                plan.getOriginCity() + " to " + plan.getDistrictName(), TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
    }

    private void addTransportSection(Document document, TourPlan plan) throws DocumentException {
        document.add(new Paragraph("Transport", HEADING_FONT));
        document.add(createTransportLine("Outbound", plan.getOutboundTransport()));
        document.add(createTransportLine("Return", plan.getReturnTransport()));
        document.add(new Paragraph(" "));
    }

    private Paragraph createTransportLine(String label, TransportOption transport) {
        if (transport == null) {
            return new Paragraph(label + ": Not selected", NORMAL_FONT);
        }
        String text = String.format("%s: %s (%s) - Departure: %s, Cost: %s/person",
                label, transport.getName(), transport.getMode(),
                transport.getDepartureTime() != null ? transport.getDepartureTime() : "TBD",
                transport.getCostPerPerson() != null ? transport.getCostPerPerson() + " tk" : "TBD");
        if (transport.getNote() != null && !transport.getNote().isBlank()) {
            text += " - " + transport.getNote();
        }
        return new Paragraph(text, NORMAL_FONT);
    }

    private void addSpotsSection(Document document, TourPlan plan) throws DocumentException {
        document.add(new Paragraph("Spots", HEADING_FONT));
        int order = 1;
        for (TouristSpot spot : plan.getSelectedSpots()) {
            String line = order + ". " + spot.getName();
            if (spot.getOffDay() != null && !spot.getOffDay().isBlank()) {
                line += " (Off day: " + spot.getOffDay() + ")";
            }
            if (spot.getTip() != null && !spot.getTip().isBlank()) {
                line += " - " + spot.getTip();
            }
            document.add(new Paragraph(line, NORMAL_FONT));
            order++;
        }
        document.add(new Paragraph(" "));
    }

    private void addFoodSection(Document document, TourPlan plan) throws DocumentException {
        document.add(new Paragraph("Khabar (Food)", HEADING_FONT));
        for (FoodPlace food : plan.getFoods()) {
            String line = food.getName();
            if (food.getLocation() != null && !food.getLocation().isBlank()) {
                line += " (" + food.getLocation() + ")";
            }
            document.add(new Paragraph("- " + line, NORMAL_FONT));
        }
        document.add(new Paragraph(" "));
    }

    private void addHotelSection(Document document, TourPlan plan) throws DocumentException {
        document.add(new Paragraph("Hotel", HEADING_FONT));
        Hotel hotel = plan.getSelectedHotel();
        if (hotel != null) {
            Integer price = hotel.getPrice(plan.isAcRoom());
            String priceText = price != null ? price + " tk/night" : "price TBD";
            document.add(new Paragraph(hotel.getName() + " (" + (plan.isAcRoom() ? "AC" : "Non-AC")
                    + " - " + priceText + ")", NORMAL_FONT));
            if (hotel.getReview() != null && !hotel.getReview().isBlank()) {
                document.add(new Paragraph(hotel.getReview(), SMALL_FONT));
            }
            if (hotel.getLocation() != null && !hotel.getLocation().isBlank()) {
                document.add(new Paragraph("Location: " + hotel.getLocation(), SMALL_FONT));
            }
        } else {
            document.add(new Paragraph("No hotel selected", NORMAL_FONT));
        }
        document.add(new Paragraph(" "));
    }

    private void addBudgetSection(Document document, TourPlan plan) throws DocumentException {
        document.add(new Paragraph("Estimate Cost", HEADING_FONT));
        BudgetBreakdown budget = plan.getBudget();
        if (budget == null) {
            document.add(new Paragraph("Budget not calculated", NORMAL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(10);

        addTableHeader(table, "Item");
        addTableHeader(table, "Detail");
        addTableHeader(table, "Amount (tk)");

        for (BudgetBreakdown.BudgetLineItem item : budget.getLineItems()) {
            addTableCell(table, item.getLabel());
            addTableCell(table, item.getDetail());
            addTableCell(table, String.valueOf(item.getAmount()));
        }

        addTableCell(table, "Total");
        addTableCell(table, "");
        addTableCell(table, String.valueOf(budget.getTotal()));

        document.add(table);

        if (!budget.getMissingCosts().isEmpty()) {
            document.add(new Paragraph("Not included: " + budget.getMissingCostsLabel(), NORMAL_FONT));
        }
    }

    private void addGuideSection(Document document, TourPlan plan) throws DocumentException {
        if (plan.getTourGuideUrl() != null && !plan.getTourGuideUrl().isBlank()) {
            document.add(new Paragraph("Tour Guide", HEADING_FONT));
            document.add(new Paragraph(plan.getTourGuideUrl(), NORMAL_FONT));
        }
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADING_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        table.addCell(new Phrase(text != null ? text : "", NORMAL_FONT));
    }
}
