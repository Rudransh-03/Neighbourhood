package com.neighbourhood.intelligence.util;

import lombok.experimental.UtilityClass;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@UtilityClass
public class GeometryUtil {

    private static final int SRID = 4326;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID);

    public Point createPoint(double lat, double lng) {
        // JTS convention: longitude = X, latitude = Y
        return GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
    }
}
