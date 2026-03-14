package com.neighbourhood.intelligence.util;

import com.neighbourhood.intelligence.entity.CongestionLevel;
import com.neighbourhood.intelligence.entity.ConnectivityData;
import com.neighbourhood.intelligence.entity.Facility;
import com.neighbourhood.intelligence.entity.FacilityType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class ScoringEngine {

    private static final double MAX_SCORE = 5.0;
    private static final double MIN_SCORE = 0.0;

    private static final double AIRPORT_EXCELLENT = 10_000;
    private static final double AIRPORT_GOOD      = 20_000;
    private static final double AIRPORT_FAIR      = 35_000;
    private static final double METRO_EXCELLENT   = 500;
    private static final double METRO_GOOD        = 1_500;
    private static final double METRO_FAIR        = 3_000;

    public BigDecimal computeLocalityScore(BigDecimal amenitiesScore,
                                           BigDecimal connectivityScore,
                                           BigDecimal trafficScore,
                                           BigDecimal userRatingScore,
                                           double amenitiesWeight,
                                           double connectivityWeight,
                                           double trafficWeight,
                                           double userRatingWeight) {
        double amenities    = amenitiesScore    != null ? amenitiesScore.doubleValue()    : 0.0;
        double connectivity = connectivityScore != null ? connectivityScore.doubleValue() : 0.0;
        double traffic      = trafficScore      != null ? trafficScore.doubleValue()      : 0.0;
        double userRating   = userRatingScore   != null ? userRatingScore.doubleValue()   : 0.0;

        double weighted = (amenities    * amenitiesWeight)
                + (connectivity * connectivityWeight)
                + (traffic      * trafficWeight)
                + (userRating   * userRatingWeight);

        return BigDecimal.valueOf(weighted).setScale(1, RoundingMode.HALF_UP);
    }

    public BigDecimal computeAmenitiesScore(List<Facility> facilities) {
        if (facilities == null || facilities.isEmpty()) return BigDecimal.ZERO;

        Map<String, Long> typeCount = facilities.stream()
                .collect(Collectors.groupingBy(Facility::getFacilityType, Collectors.counting()));

        double score = 0.0;
        score += Math.min(typeCount.getOrDefault(FacilityType.SCHOOL.name(),        0L), 3) * 0.4;
        score += Math.min(typeCount.getOrDefault(FacilityType.HOSPITAL.name(),      0L), 3) * 0.5;
        score += Math.min(typeCount.getOrDefault(FacilityType.GROCERY_STORE.name(), 0L), 5) * 0.2;
        score += Math.min(typeCount.getOrDefault(FacilityType.MALL.name(),          0L), 2) * 0.3;
        score += typeCount.getOrDefault(FacilityType.AIRPORT.name(), 0L) > 0 ? 0.5 : 0;
        score += Math.min(typeCount.getOrDefault(FacilityType.METRO_STATION.name(), 0L), 2) * 0.4;

        return clamp(score).setScale(1, RoundingMode.HALF_UP);
    }

    public BigDecimal computeConnectivityScore(ConnectivityData data) {
        if (data == null) return BigDecimal.ZERO;

        double airportScore = scoreAirport(data.getNearestAirportDistanceM());
        double metro1Score  = scoreMetro(data.getMetroStation1DistanceM());
        double metro2Score  = scoreMetro(data.getMetroStation2DistanceM());
        double avgMetro     = (metro1Score + metro2Score) / 2.0;
        double combined     = (airportScore * 0.4) + (avgMetro * 0.6);

        return clamp(combined).setScale(1, RoundingMode.HALF_UP);
    }

    public BigDecimal computeTrafficScore(String congestionLevel) {
        if (congestionLevel == null) return BigDecimal.valueOf(2.5);
        return switch (CongestionLevel.valueOf(congestionLevel)) {
            case NORMAL   -> BigDecimal.valueOf(5.0);
            case MODERATE -> BigDecimal.valueOf(3.0);
            case HEAVY    -> BigDecimal.valueOf(1.0);
        };
    }

    public BigDecimal computeUserRatingScore(BigDecimal averageRating) {
        if (averageRating == null) return BigDecimal.ZERO;
        return averageRating.setScale(1, RoundingMode.HALF_UP);
    }

    public String classifyCongestion(double multiplier) {
        if (multiplier >= 3.0) return CongestionLevel.HEAVY.name();
        if (multiplier >= 2.0) return CongestionLevel.MODERATE.name();
        return CongestionLevel.NORMAL.name();
    }

    private double scoreAirport(Double distanceM) {
        if (distanceM == null)              return 0.0;
        if (distanceM <= AIRPORT_EXCELLENT) return 5.0;
        if (distanceM <= AIRPORT_GOOD)      return 4.0;
        if (distanceM <= AIRPORT_FAIR)      return 3.0;
        return 2.0;
    }

    private double scoreMetro(Double distanceM) {
        if (distanceM == null)            return 0.0;
        if (distanceM <= METRO_EXCELLENT) return 5.0;
        if (distanceM <= METRO_GOOD)      return 4.0;
        if (distanceM <= METRO_FAIR)      return 3.0;
        return 1.5;
    }

    private BigDecimal clamp(double value) {
        return BigDecimal.valueOf(Math.max(MIN_SCORE, Math.min(MAX_SCORE, value)));
    }
}