package com.neighbourhood.intelligence.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class LocalitySearchResponse {
    private Long regionId;
    private String geohash;
    private String formattedAddress;
    private Double centroidLat;
    private Double centroidLng;
    private BigDecimal localityScore;
    private SentimentResponse sentiment;
    private String aiSummary;
    private Map<String, List<FacilityResponse>> facilities;  // changed from List to Map
    private ConnectivityResponse connectivity;
    private TrafficResponse traffic;
    private ZonedDateTime lastUpdated;
}