package com.neighbourhood.intelligence.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
public class TrafficResponse {
    private Integer baselineTimeSecs;
    private Integer peakTimeSecs;
    private BigDecimal multiplier;
    private String congestionLevel;
    private BigDecimal trafficScore;
    private ZonedDateTime lastTrafficUpdated;
}
