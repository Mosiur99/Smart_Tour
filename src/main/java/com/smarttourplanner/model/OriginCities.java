package com.smarttourplanner.model;

import java.util.List;

/** The starting cities the site supports. Anything else falls back to the default. */
public final class OriginCities {

    public static final String DEFAULT = "Dhaka";
    public static final List<String> ALL = List.of(DEFAULT, "Chattogram", "Sylhet", "Khulna", "Rajshahi");

    private OriginCities() {
    }

    public static String resolve(String city) {
        return ALL.contains(city) ? city : DEFAULT;
    }
}
