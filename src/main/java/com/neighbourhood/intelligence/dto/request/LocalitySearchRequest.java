package com.neighbourhood.intelligence.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LocalitySearchRequest {

    @NotBlank(message = "Search query must not be blank")
    @Size(min = 3, max = 500, message = "Search query must be between 3 and 500 characters")
    private String query;
}
