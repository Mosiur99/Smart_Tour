package com.smarttourplanner.model;

public class BudgetDefaults {

    private int localTransportPerPerson = 1000;
    private int foodPerPerson = 1000;

    public int getLocalTransportPerPerson() {
        return localTransportPerPerson;
    }

    public void setLocalTransportPerPerson(int localTransportPerPerson) {
        this.localTransportPerPerson = localTransportPerPerson;
    }

    public int getFoodPerPerson() {
        return foodPerPerson;
    }

    public void setFoodPerPerson(int foodPerPerson) {
        this.foodPerPerson = foodPerPerson;
    }
}
