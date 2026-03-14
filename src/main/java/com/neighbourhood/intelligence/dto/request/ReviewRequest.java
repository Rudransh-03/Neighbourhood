package com.neighbourhood.intelligence.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Region ID must not be null")
    private Long regionId;

    @NotNull(message = "Rating must not be null")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotBlank(message = "Review text must not be blank")
    @Size(min = 10, max = 2000, message = "Review text must be between 10 and 2000 characters")
    private String reviewText;
}
