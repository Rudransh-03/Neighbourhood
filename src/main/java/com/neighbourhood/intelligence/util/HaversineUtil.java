package com.neighbourhood.intelligence.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class HaversineUtil {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    public String formatDistance(double meters) {
        if (meters < 1000) {
            return "500m";
        }
        long km = Math.round(meters / 1000.0);
        return km + " km";
    }
}
