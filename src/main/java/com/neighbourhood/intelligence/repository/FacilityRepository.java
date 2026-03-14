package com.neighbourhood.intelligence.repository;

import com.neighbourhood.intelligence.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByRegionId(Long regionId);

    List<Facility> findByRegionIdAndFacilityType(Long regionId, String facilityType);

    @Modifying
    @Query("DELETE FROM Facility f WHERE f.region.id = :regionId")
    void deleteByRegionId(@Param("regionId") Long regionId);

    @Query("SELECT f FROM Facility f WHERE f.region.id = :regionId ORDER BY f.distanceMeters ASC")
    List<Facility> findByRegionIdOrderByDistance(@Param("regionId") Long regionId);
}
