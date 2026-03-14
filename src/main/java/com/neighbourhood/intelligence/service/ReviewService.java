package com.neighbourhood.intelligence.service;

import com.neighbourhood.intelligence.dto.request.ReviewRequest;
import com.neighbourhood.intelligence.dto.response.PagedResponse;
import com.neighbourhood.intelligence.dto.response.ReviewResponse;

public interface ReviewService {
    ReviewResponse submitReview(ReviewRequest request, String userId);
    PagedResponse<ReviewResponse> getReviews(Long regionId, int page, int size);
}
