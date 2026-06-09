package com.smarttourplanner.model;

public class DistrictSummary {

    private String id;
    private String name;
    private String division;
    private boolean hasDetailData;

    public DistrictSummary() {
    }

    public DistrictSummary(String id, String name, String division, boolean hasDetailData) {
        this.id = id;
        this.name = name;
        this.division = division;
        this.hasDetailData = hasDetailData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public boolean isHasDetailData() {
        return hasDetailData;
    }

    public void setHasDetailData(boolean hasDetailData) {
        this.hasDetailData = hasDetailData;
    }
}
