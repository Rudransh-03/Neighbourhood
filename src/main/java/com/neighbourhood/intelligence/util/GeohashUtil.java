package com.neighbourhood.intelligence.util;

import ch.hsr.geohash.GeoHash;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GeohashUtil {

    public String encode(double lat, double lng, int precision) {
        return GeoHash.withCharacterPrecision(lat, lng, precision).toBase32();
    }

    public double[] decode(String geohash) {
        GeoHash hash = GeoHash.fromGeohashString(geohash);
        var point = hash.getBoundingBoxCenter();
        return new double[]{point.getLatitude(), point.getLongitude()};
    }

    public double[] getBoundingBox(String geohash) {
        GeoHash hash = GeoHash.fromGeohashString(geohash);
        var bbox = hash.getBoundingBox();
        return new double[]{
                bbox.getSouthWestCorner().getLatitude(), bbox.getSouthWestCorner().getLongitude(),
                bbox.getNorthEastCorner().getLatitude(), bbox.getNorthEastCorner().getLongitude()
        };
    }
}
