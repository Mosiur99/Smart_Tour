package com.smarttourplanner.planner;

final class GeoDistance {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistance() {
    }

    static double kilometers(double lat1, double lng1, double lat2, double lng2) {
        double latGap = Math.toRadians(lat2 - lat1);
        double lngGap = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(latGap / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(lngGap / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }
}
