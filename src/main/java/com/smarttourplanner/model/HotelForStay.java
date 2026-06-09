package com.smarttourplanner.model;

public class HotelForStay {

    private String name;
    private String type;
    private String recommendedArea;
    private Integer pricePerNight;
    private String currency;
    private String verificationStatus;
    private String note;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRecommendedArea() {
        return recommendedArea;
    }

    public void setRecommendedArea(String recommendedArea) {
        this.recommendedArea = recommendedArea;
    }

    public Integer getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Integer pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDisplayName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return recommendedArea != null ? recommendedArea : "Local accommodation";
    }
}
