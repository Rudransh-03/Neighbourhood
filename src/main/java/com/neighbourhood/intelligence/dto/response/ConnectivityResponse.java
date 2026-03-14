package com.neighbourhood.intelligence.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConnectivityResponse {
    private String nearestAirportName;
    private String nearestAirportDistance;
    private String metroStation1Name;
    private String metroStation1Distance;
    private String metroStation2Name;
    private String metroStation2Distance;
    private boolean noNearbyMetro;
    private BigDecimal connectivityScore;
}