package com.smarttourplanner.model;

import java.util.ArrayList;
import java.util.List;

public class TourPlan {

    private String originCity;
    private String districtId;
    private String districtName;
    private TransportOption outboundTransport;
    private TransportOption returnTransport;
    private List<TouristSpot> selectedSpots = new ArrayList<>();
    private Hotel selectedHotel;
    private boolean acRoom;
    private int travelers;
    private int nights;
    private BudgetBreakdown budget;
    private String tourGuideUrl;
    private List<FoodPlace> foods = new ArrayList<>();

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public TransportOption getOutboundTransport() {
        return outboundTransport;
    }

    public void setOutboundTransport(TransportOption outboundTransport) {
        this.outboundTransport = outboundTransport;
    }

    public TransportOption getReturnTransport() {
        return returnTransport;
    }

    public void setReturnTransport(TransportOption returnTransport) {
        this.returnTransport = returnTransport;
    }

    public List<TouristSpot> getSelectedSpots() {
        return selectedSpots;
    }

    public void setSelectedSpots(List<TouristSpot> selectedSpots) {
        this.selectedSpots = selectedSpots != null ? selectedSpots : new ArrayList<>();
    }

    public Hotel getSelectedHotel() {
        return selectedHotel;
    }

    public void setSelectedHotel(Hotel selectedHotel) {
        this.selectedHotel = selectedHotel;
    }

    public boolean isAcRoom() {
        return acRoom;
    }

    public void setAcRoom(boolean acRoom) {
        this.acRoom = acRoom;
    }

    public int getTravelers() {
        return travelers;
    }

    public void setTravelers(int travelers) {
        this.travelers = travelers;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public BudgetBreakdown getBudget() {
        return budget;
    }

    public void setBudget(BudgetBreakdown budget) {
        this.budget = budget;
    }

    public String getTourGuideUrl() {
        return tourGuideUrl;
    }

    public void setTourGuideUrl(String tourGuideUrl) {
        this.tourGuideUrl = tourGuideUrl;
    }

    public List<FoodPlace> getFoods() {
        return foods;
    }

    public void setFoods(List<FoodPlace> foods) {
        this.foods = foods != null ? foods : new ArrayList<>();
    }
}
