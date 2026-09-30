package com.resqcloud.common.utils;

public final class GeoLocationUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoLocationUtils() {}

    /**
     * Calculates great-circle distance between two points on the Earth's surface using Haversine formula.
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in kilometers
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return Math.round((EARTH_RADIUS_KM * c) * 100.0) / 100.0;
    }

    public static boolean isValidCoordinate(Double lat, Double lon) {
        if (lat == null || lon == null) return false;
        return lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0;
    }
}
