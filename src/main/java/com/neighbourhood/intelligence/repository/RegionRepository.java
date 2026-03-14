package com.neighbourhood.intelligence.repository;

import com.neighbourhood.intelligence.entity.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByGeohash(String geohash);

    @Query("SELECT r FROM Region r " +
            "LEFT JOIN FETCH r.connectivityData " +
            "LEFT JOIN FETCH r.trafficData " +
            "WHERE r.id = :id")
    Optional<Region> findByIdWithDetails(@Param("id") Long id);

    boolean existsByGeohash(String geohash);

    @Query("SELECT r FROM Region r WHERE r.lastUpdated IS NULL OR r.lastUpdated < :cutoff")
    Page<Region> findStaleRegions(@Param("cutoff") ZonedDateTime cutoff, Pageable pageable);

    @Query("SELECT r FROM Region r WHERE r.lastSummaryUpdated IS NULL OR r.lastSummaryUpdated < :cutoff")
    Page<Region> findRegionsWithStaleSummary(@Param("cutoff") ZonedDateTime cutoff, Pageable pageable);

    @Query("SELECT r FROM Region r LEFT JOIN r.trafficData t " +
            "WHERE t IS NULL OR t.lastTrafficUpdated IS NULL OR t.lastTrafficUpdated < :cutoff")
    Page<Region> findRegionsWithStaleTraffic(@Param("cutoff") ZonedDateTime cutoff, Pageable pageable);
}