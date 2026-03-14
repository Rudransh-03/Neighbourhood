package com.neighbourhood.intelligence.service.external.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlaceResult {
    private String placeId;
    private String name;
    private double lat;
    private double lng;
    private BigDecimal rating;
}
