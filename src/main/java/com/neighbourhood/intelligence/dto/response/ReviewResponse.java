package com.neighbourhood.intelligence.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long regionId;
    private String userId;
    private Integer rating;
    private String reviewText;
    private ZonedDateTime createdAt;
}
