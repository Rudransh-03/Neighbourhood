package com.neighbourhood.intelligence.repository;

import com.neighbourhood.intelligence.entity.TrafficData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrafficDataRepository extends JpaRepository<TrafficData, Long> {

    Optional<TrafficData> findByRegionId(Long regionId);
}
