package com.neighbourhood.intelligence.service.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResult implements Serializable {
    private String placeId;
    private String name;
    private double lat;
    private double lng;
    private BigDecimal rating;
}
