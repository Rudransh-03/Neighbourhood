package com.neighbourhood.intelligence.repository;

import com.neighbourhood.intelligence.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByRegionIdAndUserId(Long regionId, String userId);

    boolean existsByRegionIdAndUserId(Long regionId, String userId);

    Page<Review> findByRegionId(Long regionId, Pageable pageable);

    List<Review> findByRegionId(Long regionId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.region.id = :regionId")
    long countByRegionId(@Param("regionId") Long regionId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.region.id = :regionId")
    Double averageRatingByRegionId(@Param("regionId") Long regionId);
}
