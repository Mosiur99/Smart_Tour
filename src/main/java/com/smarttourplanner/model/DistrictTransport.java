package com.smarttourplanner.model;

import java.util.ArrayList;
import java.util.List;

public class DistrictTransport {

    private List<TransportOption> fromDhaka = new ArrayList<>();
    private List<TransportOption> returnToDhaka = new ArrayList<>();

    public List<TransportOption> getFromDhaka() {
        return fromDhaka;
    }

    public void setFromDhaka(List<TransportOption> fromDhaka) {
        this.fromDhaka = fromDhaka != null ? fromDhaka : new ArrayList<>();
    }

    public List<TransportOption> getReturnToDhaka() {
        return returnToDhaka;
    }

    public void setReturnToDhaka(List<TransportOption> returnToDhaka) {
        this.returnToDhaka = returnToDhaka != null ? returnToDhaka : new ArrayList<>();
    }
}
