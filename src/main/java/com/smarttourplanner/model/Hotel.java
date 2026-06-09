package com.smarttourplanner.model;

public class Hotel {

    private String name;
    private Integer acPrice;
    private Integer nonAcPrice;
    private String review;
    private String location;
    private String website;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAcPrice() {
        return acPrice;
    }

    public void setAcPrice(Integer acPrice) {
        this.acPrice = acPrice;
    }

    public Integer getNonAcPrice() {
        return nonAcPrice;
    }

    public void setNonAcPrice(Integer nonAcPrice) {
        this.nonAcPrice = nonAcPrice;
    }

    public int getAcPriceOrZero() {
        return acPrice != null ? acPrice : 0;
    }

    public int getNonAcPriceOrZero() {
        return nonAcPrice != null ? nonAcPrice : 0;
    }

    public boolean hasPrice() {
        return acPrice != null || nonAcPrice != null;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPriceLabel() {
        if (acPrice != null && nonAcPrice != null) {
            return "AC: " + acPrice + " tk | Non-AC: " + nonAcPrice + " tk";
        }
        if (nonAcPrice != null) {
            return "From " + nonAcPrice + " tk/night";
        }
        if (acPrice != null) {
            return "AC from " + acPrice + " tk/night";
        }
        return "Price TBD — verify before booking";
    }
}
