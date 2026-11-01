package com.smarttourplanner.model;

import java.util.ArrayList;
import java.util.List;

public class BudgetBreakdown {

    private int transportCost;
    private int hotelCost;
    private int localTransportCost;
    private int foodCost;
    private int total;
    private String transportDetail;
    private String hotelDetail;
    private String localTransportDetail;
    private String foodDetail;
    private final List<String> missingCosts = new ArrayList<>();

    public int getTransportCost() {
        return transportCost;
    }

    public void setTransportCost(int transportCost) {
        this.transportCost = transportCost;
    }

    public int getHotelCost() {
        return hotelCost;
    }

    public void setHotelCost(int hotelCost) {
        this.hotelCost = hotelCost;
    }

    public int getLocalTransportCost() {
        return localTransportCost;
    }

    public void setLocalTransportCost(int localTransportCost) {
        this.localTransportCost = localTransportCost;
    }

    public int getFoodCost() {
        return foodCost;
    }

    public void setFoodCost(int foodCost) {
        this.foodCost = foodCost;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getTransportDetail() {
        return transportDetail;
    }

    public void setTransportDetail(String transportDetail) {
        this.transportDetail = transportDetail;
    }

    public String getHotelDetail() {
        return hotelDetail;
    }

    public void setHotelDetail(String hotelDetail) {
        this.hotelDetail = hotelDetail;
    }

    public String getLocalTransportDetail() {
        return localTransportDetail;
    }

    public void setLocalTransportDetail(String localTransportDetail) {
        this.localTransportDetail = localTransportDetail;
    }

    public String getFoodDetail() {
        return foodDetail;
    }

    public void setFoodDetail(String foodDetail) {
        this.foodDetail = foodDetail;
    }

    public List<String> getMissingCosts() {
        return List.copyOf(missingCosts);
    }

    public void addMissingCost(String cost) {
        missingCosts.add(cost);
    }

    public String getMissingCostsLabel() {
        return String.join(", ", missingCosts);
    }

    public List<BudgetLineItem> getLineItems() {
        List<BudgetLineItem> items = new ArrayList<>();
        items.add(new BudgetLineItem("Transport (round trip)", transportDetail, transportCost));
        items.add(new BudgetLineItem("Hotel", hotelDetail, hotelCost));
        items.add(new BudgetLineItem("Local transport", localTransportDetail, localTransportCost));
        items.add(new BudgetLineItem("Food", foodDetail, foodCost));
        return items;
    }

    public static class BudgetLineItem {
        private final String label;
        private final String detail;
        private final int amount;

        public BudgetLineItem(String label, String detail, int amount) {
            this.label = label;
            this.detail = detail;
            this.amount = amount;
        }

        public String getLabel() {
            return label;
        }

        public String getDetail() {
            return detail;
        }

        public int getAmount() {
            return amount;
        }
    }
}
