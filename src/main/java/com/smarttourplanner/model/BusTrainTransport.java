package com.smarttourplanner.model;

import java.util.ArrayList;
import java.util.List;

public class BusTrainTransport {

    private List<TransportOption> bus = new ArrayList<>();
    private List<TransportOption> train = new ArrayList<>();
    private String note;

    public List<TransportOption> getBus() {
        return bus;
    }

    public void setBus(List<TransportOption> bus) {
        this.bus = bus != null ? bus : new ArrayList<>();
    }

    public List<TransportOption> getTrain() {
        return train;
    }

    public void setTrain(List<TransportOption> train) {
        this.train = train != null ? train : new ArrayList<>();
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
