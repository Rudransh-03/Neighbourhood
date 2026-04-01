package com.neighbourhood.intelligence.service.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceMatrixResult implements Serializable {
    private int durationSeconds;
    private int distanceMeters;
}
