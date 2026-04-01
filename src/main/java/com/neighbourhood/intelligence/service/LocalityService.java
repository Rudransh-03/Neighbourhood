package com.neighbourhood.intelligence.service;

import com.neighbourhood.intelligence.dto.request.LocalitySearchRequest;
import com.neighbourhood.intelligence.dto.response.LocalitySearchResponse;

public interface LocalityService {
    LocalitySearchResponse search(LocalitySearchRequest request);
    LocalitySearchResponse getByRegionId(Long regionId);
    LocalitySearchResponse getAiSummary(Long regionId);
}
