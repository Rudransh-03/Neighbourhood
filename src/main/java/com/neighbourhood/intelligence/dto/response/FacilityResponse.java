package com.neighbourhood.intelligence.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FacilityResponse {
    private Long id;
    private String name;
    private String facilityType;
    private Double lat;
    private Double lng;
    private BigDecimal rating;
    private String distanceDisplay;
    private Double distanceMeters;
    private boolean unavailable;
    private String unavailableMessage;
}