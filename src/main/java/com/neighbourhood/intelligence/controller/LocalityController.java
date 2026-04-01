package com.neighbourhood.intelligence.controller;

import com.neighbourhood.intelligence.dto.request.LocalitySearchRequest;
import com.neighbourhood.intelligence.dto.response.ApiResponse;
import com.neighbourhood.intelligence.dto.response.LocalitySearchResponse;
import com.neighbourhood.intelligence.service.LocalityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locality")
@RequiredArgsConstructor
@Slf4j
public class LocalityController {

    private final LocalityService localityService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<LocalitySearchResponse>> search(
            @Valid @RequestBody LocalitySearchRequest request) {
        log.info("POST /api/locality/search query={}", request.getQuery());
        LocalitySearchResponse response = localityService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{regionId}")
    public ResponseEntity<ApiResponse<LocalitySearchResponse>> getByRegionId(
            @PathVariable Long regionId) {
        log.info("GET /api/locality/{}", regionId);
        LocalitySearchResponse response = localityService.getByRegionId(regionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{regionId}/summary")
    public ResponseEntity<ApiResponse<LocalitySearchResponse>> getAiSummary(
            @PathVariable Long regionId) {
        log.info("GET /api/locality/{}/summary - Fetching lazy AI text", regionId);
        LocalitySearchResponse response = localityService.getAiSummary(regionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
