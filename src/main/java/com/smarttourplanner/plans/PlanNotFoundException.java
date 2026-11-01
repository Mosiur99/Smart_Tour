package com.smarttourplanner.plans;

public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException(String message) {
        super(message);
    }
}
