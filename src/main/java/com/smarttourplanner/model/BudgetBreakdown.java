package com.smarttourplanner.model;

import java.util.ArrayList;
import java.util.List;

public class BudgetBreakdown {

    private int trainCost;
    private int hotelCost;
    private int localTransportCost;
    private int foodCost;
    private int total;
    private String trainDetail;
    private String hotelDetail;
    private String localTransportDetail;
    private String foodDetail;

    public int getTrainCost() {
        return trainCost;
    }

    public void setTrainCost(int trainCost) {
        this.trainCost = trainCost;
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

    public String getTrainDetail() {
        return trainDetail;
    }

    public void setTrainDetail(String trainDetail) {
        this.trainDetail = trainDetail;
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

    public List<BudgetLineItem> getLineItems() {
        List<BudgetLineItem> items = new ArrayList<>();
        items.add(new BudgetLineItem("Transport (round trip)", trainDetail, trainCost));
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
