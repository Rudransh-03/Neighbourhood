package com.neighbourhood.intelligence.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SentimentResponse {
    private BigDecimal averageRating;
    private Integer reviewCount;
    private String message;
    private boolean hasSufficientData;
}
