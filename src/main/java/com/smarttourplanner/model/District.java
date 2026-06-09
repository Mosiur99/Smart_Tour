package com.smarttourplanner.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class District {

    private String id;
    private String name;
    private String division;
    private DistrictTransport transport = new DistrictTransport();
    private List<TouristSpot> spots = new ArrayList<>();
    private List<Hotel> hotels = new ArrayList<>();
    private List<FoodPlace> foods = new ArrayList<>();
    private BudgetDefaults budgetDefaults = new BudgetDefaults();
    private String tourGuideUrl;
    private String hotelNote;
    private String travelNote;
    private List<TouristSpot> historicalPlaces = new ArrayList<>();
    private List<University> popularUniversities = new ArrayList<>();
    private List<HotelForStay> hotelsForStay = new ArrayList<>();
    private BusTrainTransport busTrainTransport = new BusTrainTransport();
    private String universityNote;

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

    public DistrictTransport getTransport() {
        return transport;
    }

    public void setTransport(DistrictTransport transport) {
        this.transport = transport != null ? transport : new DistrictTransport();
    }

    public List<TouristSpot> getSpots() {
        return spots;
    }

    public void setSpots(List<TouristSpot> spots) {
        this.spots = spots != null ? spots : new ArrayList<>();
    }

    public List<Hotel> getHotels() {
        return hotels;
    }

    public void setHotels(List<Hotel> hotels) {
        this.hotels = hotels != null ? hotels : new ArrayList<>();
    }

    public List<FoodPlace> getFoods() {
        return foods;
    }

    public void setFoods(List<FoodPlace> foods) {
        this.foods = foods != null ? foods : new ArrayList<>();
    }

    public BudgetDefaults getBudgetDefaults() {
        return budgetDefaults;
    }

    public void setBudgetDefaults(BudgetDefaults budgetDefaults) {
        this.budgetDefaults = budgetDefaults != null ? budgetDefaults : new BudgetDefaults();
    }

    public String getTourGuideUrl() {
        return tourGuideUrl;
    }

    public void setTourGuideUrl(String tourGuideUrl) {
        this.tourGuideUrl = tourGuideUrl;
    }

    public String getHotelNote() {
        return hotelNote;
    }

    public void setHotelNote(String hotelNote) {
        this.hotelNote = hotelNote;
    }

    public String getTravelNote() {
        return travelNote;
    }

    public void setTravelNote(String travelNote) {
        this.travelNote = travelNote;
    }

    public List<TouristSpot> getHistoricalPlaces() {
        return historicalPlaces;
    }

    public void setHistoricalPlaces(List<TouristSpot> historicalPlaces) {
        this.historicalPlaces = historicalPlaces != null ? historicalPlaces : new ArrayList<>();
    }

    public List<University> getPopularUniversities() {
        return popularUniversities;
    }

    public void setPopularUniversities(List<University> popularUniversities) {
        this.popularUniversities = popularUniversities != null ? popularUniversities : new ArrayList<>();
    }

    public List<HotelForStay> getHotelsForStay() {
        return hotelsForStay;
    }

    public void setHotelsForStay(List<HotelForStay> hotelsForStay) {
        this.hotelsForStay = hotelsForStay != null ? hotelsForStay : new ArrayList<>();
    }

    public BusTrainTransport getBusTrainTransport() {
        return busTrainTransport;
    }

    public void setBusTrainTransport(BusTrainTransport busTrainTransport) {
        this.busTrainTransport = busTrainTransport != null ? busTrainTransport : new BusTrainTransport();
    }

    public String getUniversityNote() {
        return universityNote;
    }

    public void setUniversityNote(String universityNote) {
        this.universityNote = universityNote;
    }

    public List<TouristSpot> getVisitOptions() {
        List<TouristSpot> options = new ArrayList<>(spots);
        Set<String> names = new HashSet<>();
        for (TouristSpot spot : spots) {
            names.add(spot.getName().toLowerCase());
        }
        for (TouristSpot place : historicalPlaces) {
            if (names.add(place.getName().toLowerCase())) {
                options.add(place);
            }
        }
        int priority = 50;
        for (University university : popularUniversities) {
            if (names.add(university.getName().toLowerCase())) {
                TouristSpot spot = new TouristSpot();
                spot.setName(university.getName());
                spot.setArea("university");
                spot.setPriority(priority++);
                String tip = university.getLocation();
                if (university.getNote() != null && !university.getNote().isBlank()) {
                    tip = tip != null && !tip.isBlank() ? tip + " - " + university.getNote() : university.getNote();
                }
                spot.setTip(tip);
                options.add(spot);
            }
        }
        return options;
    }
}
