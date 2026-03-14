package com.neighbourhood.intelligence.controller;

import com.neighbourhood.intelligence.dto.request.ReviewRequest;
import com.neighbourhood.intelligence.dto.response.ApiResponse;
import com.neighbourhood.intelligence.dto.response.PagedResponse;
import com.neighbourhood.intelligence.dto.response.ReviewResponse;
import com.neighbourhood.intelligence.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/review")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @Valid @RequestBody ReviewRequest request,
            @RequestHeader("X-User-Id")
            @NotBlank(message = "X-User-Id header is required")
            @Pattern(
                    regexp = "^[a-zA-Z0-9_\\-]{8,128}$",
                    message = "X-User-Id must be 8–128 alphanumeric characters"
            )
            String userId) {
        log.info("POST /api/review userId={} regionId={}", userId, request.getRegionId());
        ReviewResponse response = reviewService.submitReview(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Review submitted successfully"));
    }

    @GetMapping("/reviews/{regionId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getReviews(
            @PathVariable Long regionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/reviews/{} page={} size={}", regionId, page, size);
        PagedResponse<ReviewResponse> response = reviewService.getReviews(regionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}