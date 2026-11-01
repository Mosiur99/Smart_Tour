package com.smarttourplanner.model;

public class TransportOption {

    private String mode;
    private String name;
    private String departureTime;
    private String ticketBookingNote;
    private Integer costPerPerson;
    private String note;
    private String costVerificationStatus;
    private String costEstimateNote;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getTicketBookingNote() {
        return ticketBookingNote;
    }

    public void setTicketBookingNote(String ticketBookingNote) {
        this.ticketBookingNote = ticketBookingNote;
    }

    public Integer getCostPerPerson() {
        return costPerPerson;
    }

    public void setCostPerPerson(Integer costPerPerson) {
        this.costPerPerson = costPerPerson;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCostVerificationStatus() {
        return costVerificationStatus;
    }

    public void setCostVerificationStatus(String costVerificationStatus) {
        this.costVerificationStatus = costVerificationStatus;
    }

    public String getCostEstimateNote() {
        return costEstimateNote;
    }

    public void setCostEstimateNote(String costEstimateNote) {
        this.costEstimateNote = costEstimateNote;
    }

    public String getDisplayLabel() {
        String cost = costPerPerson != null ? costPerPerson + " tk (est.)" : "fare TBD";
        String time = departureTime != null && !departureTime.isBlank() ? departureTime : "time TBD";
        return name + " (" + mode + ", " + cost + ", " + time + ")";
    }
}
