package com.neighbourhood.intelligence.repository;

import com.neighbourhood.intelligence.entity.GeocodingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeocodingCacheRepository extends JpaRepository<GeocodingCache, Long> {

    Optional<GeocodingCache> findByQueryKey(String queryKey);
}
