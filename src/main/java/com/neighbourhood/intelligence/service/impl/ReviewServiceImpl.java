package com.neighbourhood.intelligence.service.impl;

import com.neighbourhood.intelligence.dto.request.ReviewRequest;
import com.neighbourhood.intelligence.dto.response.PagedResponse;
import com.neighbourhood.intelligence.dto.response.ReviewResponse;
import com.neighbourhood.intelligence.entity.Region;
import com.neighbourhood.intelligence.entity.Review;
import com.neighbourhood.intelligence.exception.DuplicateReviewException;
import com.neighbourhood.intelligence.exception.ProfanityException;
import com.neighbourhood.intelligence.exception.ResourceNotFoundException;
import com.neighbourhood.intelligence.repository.RegionRepository;
import com.neighbourhood.intelligence.repository.ReviewRepository;
import com.neighbourhood.intelligence.service.RegionDataService;
import com.neighbourhood.intelligence.service.ReviewService;
import com.neighbourhood.intelligence.util.ProfanityFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RegionRepository regionRepository;
    private final RegionDataService regionDataService;
    private final ProfanityFilter profanityFilter;  // injected as @Component now

    @Override
    @Transactional
    public ReviewResponse submitReview(ReviewRequest request, String userId) {
        log.info("User {} submitting review for region {}", userId, request.getRegionId());

        if (reviewRepository.existsByRegionIdAndUserId(request.getRegionId(), userId)) {
            throw new DuplicateReviewException(
                    "User has already submitted a review for this region");
        }

        if (profanityFilter.containsProfanity(request.getReviewText())) {
            throw new ProfanityException("Review contains inappropriate content");
        }

        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new ResourceNotFoundException("Region", request.getRegionId()));

        Review review = Review.builder()
                .region(region)
                .userId(userId)
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .build();

        review = reviewRepository.save(review);
        log.info("Review {} saved for region {}", review.getId(), region.getId());

        regionDataService.recalculateScores(region);

        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getReviews(Long regionId, int page, int size) {
        if (!regionRepository.existsById(regionId)) {
            throw new ResourceNotFoundException("Region", regionId);
        }

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByRegionId(regionId, pageable);

        return PagedResponse.<ReviewResponse>builder()
                .content(reviewPage.getContent().stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .page(page)
                .size(size)
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .regionId(review.getRegion().getId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .reviewText(review.getReviewText())
                .createdAt(review.getCreatedAt())
                .build();
    }
}