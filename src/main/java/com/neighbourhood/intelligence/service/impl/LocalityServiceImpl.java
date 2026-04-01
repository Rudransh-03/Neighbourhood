package com.neighbourhood.intelligence.service.impl;

import com.neighbourhood.intelligence.config.AppProperties;
import com.neighbourhood.intelligence.dto.request.LocalitySearchRequest;
import com.neighbourhood.intelligence.dto.response.*;
import com.neighbourhood.intelligence.entity.*;
import com.neighbourhood.intelligence.exception.ResourceNotFoundException;
import com.neighbourhood.intelligence.repository.*;
import com.neighbourhood.intelligence.service.LocalityService;
import com.neighbourhood.intelligence.service.RegionDataService;
import com.neighbourhood.intelligence.service.external.GoogleGeocodingService;
import com.neighbourhood.intelligence.service.external.model.GeocodingResult;
import com.neighbourhood.intelligence.util.GeohashUtil;
import com.neighbourhood.intelligence.util.HaversineUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalityServiceImpl implements LocalityService {

    private final RegionRepository regionRepository;
    private final EntityManager entityManager;
    private final FacilityRepository facilityRepository;
    private final ConnectivityDataRepository connectivityDataRepository;
    private final TrafficDataRepository trafficDataRepository;

    private final GoogleGeocodingService geocodingService;
    private final RegionDataService regionDataService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public LocalitySearchResponse search(LocalitySearchRequest request) {
        log.info("Locality search initiated for query: {}", request.getQuery());

        GeocodingResult geocoded        = geocodingService.geocode(request.getQuery());
        double          lat             = geocoded.getLat();
        double          lng             = geocoded.getLng();
        String          formattedAddress = geocoded.getFormattedAddress();

        String geohash = GeohashUtil.encode(lat, lng, appProperties.getSearch().getGeohashPrecision());
        log.info("Geohash computed: {} for lat={}, lng={}", geohash, lat, lng);

        Optional<Region> existingOpt = regionRepository.findByGeohash(geohash);
        Region region;

        ZonedDateTime now          = ZonedDateTime.now();
        int regionTtlDays          = appProperties.getCache().getRegionTtlDays();
        int trafficTtlDays         = appProperties.getCache().getTrafficTtlDays();
        int summaryTtlDays         = appProperties.getCache().getSummaryTtlDays();

        if (existingOpt.isPresent()) {
            region = existingOpt.get();

            if (region.getFormattedAddress() == null && formattedAddress != null) {
                region.setFormattedAddress(formattedAddress);
                regionRepository.save(region);
            }

            boolean regionStale = region.getLastUpdated() == null ||
                    region.getLastUpdated().isBefore(now.minusDays(regionTtlDays));
            if (regionStale) {
                log.info("Region {} is stale, refreshing facilities", geohash);
                regionDataService.refreshFacilities(region);
            } else {
                log.info("Region {} is fresh, using cached data", geohash);
            }
        } else {
            log.info("Region {} not found, creating and fetching data", geohash);
            region = regionDataService.fetchAndRefreshRegion(lat, lng, geohash, formattedAddress);
        }

        refreshTrafficIfStale(region, now, trafficTtlDays);

        // We no longer trigger refreshSummaryIfStale here; it is lazy-loaded via getAiSummary(Long)
        regionDataService.recalculateScores(region);
        Long regionId = region.getId();
        Region refreshedRegion = regionRepository.findByIdWithDetails(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));

        return buildResponse(refreshedRegion);
    }

    @Override
    @Transactional
    public LocalitySearchResponse getByRegionId(Long regionId) {
        Region region = regionRepository.findByIdWithDetails(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));

        ZonedDateTime now      = ZonedDateTime.now();
        int regionTtlDays      = appProperties.getCache().getRegionTtlDays();
        int trafficTtlDays     = appProperties.getCache().getTrafficTtlDays();
        int summaryTtlDays     = appProperties.getCache().getSummaryTtlDays();

        boolean regionStale = region.getLastUpdated() == null ||
                region.getLastUpdated().isBefore(now.minusDays(regionTtlDays));
        if (regionStale) {
            log.info("Region {} is stale on direct fetch, refreshing", regionId);
            regionDataService.refreshFacilities(region);
        }

        refreshTrafficIfStale(region, now, trafficTtlDays);
        // We no longer trigger refreshSummaryIfStale here; it is lazy-loaded via getAiSummary(Long)
        
        regionDataService.recalculateScores(region);

        Region refreshedRegion = regionRepository.findByIdWithDetails(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));

        return buildResponse(refreshedRegion);
    }

    @Override
    @Transactional
    public LocalitySearchResponse getAiSummary(Long regionId) {
        Region region = regionRepository.findByIdWithDetails(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));

        ZonedDateTime now = ZonedDateTime.now();
        int summaryTtlDays = appProperties.getCache().getSummaryTtlDays();

        // 1. Always demand fetch or refresh the AI summary here
        log.info("Lazy loading AI Summary invoked for region {}", regionId);
        refreshSummaryIfStale(region, now, summaryTtlDays);

        // 2. Return the rebuilt full payload (includes fresh summary string)
        Region refreshedRegion = regionRepository.findByIdWithDetails(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", regionId));

        return buildResponse(refreshedRegion);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void refreshTrafficIfStale(Region region, ZonedDateTime now, int ttlDays) {
        entityManager.detach(region); // force fresh read
        TrafficData trafficData = trafficDataRepository.findByRegionId(region.getId()).orElse(null);
        boolean stale = trafficData == null ||
                trafficData.getLastTrafficUpdated() == null ||
                trafficData.getLastTrafficUpdated().isBefore(now.minusDays(ttlDays));
        if (stale) {
            try {
                log.info("Traffic data stale for region {}, refreshing", region.getGeohash());
                regionDataService.refreshTraffic(region);
            } catch (Exception e) {
                log.warn("Failed to refresh traffic for region {}, skipping: {}", region.getGeohash(), e.getMessage());
            }
        }
    }

    private void refreshSummaryIfStale(Region region, ZonedDateTime now, int ttlDays) {
        boolean stale = region.getLastSummaryUpdated() == null ||
                region.getLastSummaryUpdated().isBefore(now.minusDays(ttlDays));
        if (stale) {
            try {
                regionDataService.refreshAiSummary(region);
            } catch (Exception e) {
                log.warn("Failed to refresh AI summary for region {}, skipping: {}", region.getGeohash(), e.getMessage());
            }
        }
    }

    private LocalitySearchResponse buildResponse(Region region) {
        List<Facility>   facilities = facilityRepository.findByRegionIdOrderByDistance(region.getId());
        ConnectivityData conn       = connectivityDataRepository.findByRegionId(region.getId()).orElse(null);
        TrafficData      traffic    = trafficDataRepository.findByRegionId(region.getId()).orElse(null);

        double metroThreshold = appProperties.getThresholds().getMetroMaxDistanceM();
        double mallThreshold  = appProperties.getThresholds().getMallMaxDistanceM();

        int minReviews = appProperties.getScoring().getMinReviewsForSentiment();
        boolean hasSufficientData = region.getReviewCount() != null &&
                region.getReviewCount() >= minReviews;

        SentimentResponse sentiment = SentimentResponse.builder()
                .averageRating(hasSufficientData ? region.getAverageRating() : null)
                .reviewCount(region.getReviewCount())
                .hasSufficientData(hasSufficientData)
                .message(hasSufficientData ? null : "Insufficient user data")
                .build();

        return LocalitySearchResponse.builder()
                .regionId(region.getId())
                .geohash(region.getGeohash())
                .formattedAddress(region.getFormattedAddress())
                .centroidLat(region.getCentroidLat())
                .centroidLng(region.getCentroidLng())
                .localityScore(region.getLocalityScore())
                .sentiment(sentiment)
                .aiSummary(region.getSummaryText())
                .facilities(buildFacilityResponses(facilities, metroThreshold, mallThreshold))
                .connectivity(conn != null ? mapConnectivity(conn) : null)
                .traffic(traffic != null ? mapTraffic(traffic) : null)
                .lastUpdated(region.getLastUpdated())
                .build();
    }

    private Map<String, List<FacilityResponse>> buildFacilityResponses(List<Facility> facilities,
                                                                       double metroThreshold,
                                                                       double mallThreshold) {
        Map<String, List<FacilityResponse>> grouped = new LinkedHashMap<>();

        // Initialize all categories in a fixed order
        for (FacilityType type : FacilityType.values()) {
            grouped.put(type.name(), new ArrayList<>());
        }

        boolean hasAnyMetro = false;
        boolean hasAnyMall  = false;

        for (Facility f : facilities) {
            boolean isMall  = FacilityType.MALL.name().equals(f.getFacilityType());
            boolean isMetro = FacilityType.METRO_STATION.name().equals(f.getFacilityType());

            if (isMetro) hasAnyMetro = true;
            if (isMall)  hasAnyMall  = true;

            double dist = f.getDistanceMeters() != null ? f.getDistanceMeters() : Double.MAX_VALUE;

            if (isMetro && dist > metroThreshold) {
                grouped.get(FacilityType.METRO_STATION.name())
                        .add(unavailableEntry(FacilityType.METRO_STATION.name(), "No nearby metro station"));
            } else if (isMall && dist > mallThreshold) {
                grouped.get(FacilityType.MALL.name())
                        .add(unavailableEntry(FacilityType.MALL.name(), "No nearby mall"));
            } else {
                grouped.get(f.getFacilityType()).add(mapFacility(f));
            }
        }

        if (!hasAnyMetro) grouped.get(FacilityType.METRO_STATION.name())
                .add(unavailableEntry(FacilityType.METRO_STATION.name(), "No nearby metro station"));
        if (!hasAnyMall) grouped.get(FacilityType.MALL.name())
                .add(unavailableEntry(FacilityType.MALL.name(), "No nearby mall"));

        return grouped;
    }

    private FacilityResponse unavailableEntry(String facilityType, String message) {
        return FacilityResponse.builder()
                .facilityType(facilityType)
                .unavailable(true)
                .unavailableMessage(message)
                .build();
    }

    private FacilityResponse mapFacility(Facility f) {
        return FacilityResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .facilityType(f.getFacilityType())
                .lat(f.getLat())
                .lng(f.getLng())
                .rating(f.getRating())
                .distanceMeters(f.getDistanceMeters())
                .distanceDisplay(f.getDistanceMeters() != null
                        ? HaversineUtil.formatDistance(f.getDistanceMeters()) : null)
                .unavailable(false)
                .build();
    }

    private ConnectivityResponse mapConnectivity(ConnectivityData c) {
        boolean noNearbyMetro = c.getMetroStation1Name() == null;
        return ConnectivityResponse.builder()
                .nearestAirportName(c.getNearestAirportName())
                .nearestAirportDistance(c.getNearestAirportDistanceM() != null
                        ? HaversineUtil.formatDistance(c.getNearestAirportDistanceM()) : null)
                .metroStation1Name(noNearbyMetro ? null : c.getMetroStation1Name())
                .metroStation1Distance(noNearbyMetro ? null
                        : HaversineUtil.formatDistance(c.getMetroStation1DistanceM()))
                .metroStation2Name(c.getMetroStation2Name())
                .metroStation2Distance(c.getMetroStation2DistanceM() != null
                        ? HaversineUtil.formatDistance(c.getMetroStation2DistanceM()) : null)
                .noNearbyMetro(noNearbyMetro)
                .connectivityScore(c.getConnectivityScore())
                .build();
    }

    private TrafficResponse mapTraffic(TrafficData t) {
        return TrafficResponse.builder()
                .baselineTimeSecs(t.getBaselineTimeSecs())
                .peakTimeSecs(t.getPeakTimeSecs())
                .multiplier(t.getMultiplier())
                .congestionLevel(t.getCongestionLevel())
                .trafficScore(t.getTrafficScore())
                .lastTrafficUpdated(t.getLastTrafficUpdated())
                .build();
    }
}