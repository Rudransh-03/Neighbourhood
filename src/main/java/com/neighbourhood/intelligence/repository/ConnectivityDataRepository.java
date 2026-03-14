package com.neighbourhood.intelligence.repository;

import com.neighbourhood.intelligence.entity.ConnectivityData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConnectivityDataRepository extends JpaRepository<ConnectivityData, Long> {

    Optional<ConnectivityData> findByRegionId(Long regionId);
}
