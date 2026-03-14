package com.neighbourhood.intelligence.service.external.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DistanceMatrixResult {
    private int durationSeconds;
    private int distanceMeters;
}
