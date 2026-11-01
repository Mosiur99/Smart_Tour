package com.smarttourplanner.controller;

public class DistrictNotFoundException extends RuntimeException {

    public DistrictNotFoundException(String districtId) {
        super("District not found: " + districtId);
    }
}
