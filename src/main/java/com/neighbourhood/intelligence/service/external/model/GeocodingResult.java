package com.neighbourhood.intelligence.service.external.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeocodingResult {
    private double lat;
    private double lng;
    private String formattedAddress;
}
