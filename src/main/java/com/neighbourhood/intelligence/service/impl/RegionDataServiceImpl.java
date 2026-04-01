package com.neighbourhood.intelligence.service.impl;

import com.neighbourhood.intelligence.config.AppProperties;
import com.neighbourhood.intelligence.entity.*;
import com.neighbourhood.intelligence.exception.ExternalApiException;
import com.neighbourhood.intelligence.repository.*;
import com.neighbourhood.intelligence.service.RegionDataService;
import com.neighbourhood.intelligence.service.external.*;
import com.neighbourhood.intelligence.service.external.model.DistanceMatrixResult;
import com.neighbourhood.intelligence.service.external.model.PlaceResult;
import com.neighbourhood.intelligence.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegionDataServiceImpl implements RegionDataService {

    private final RegionRepository regionRepository;
    private final FacilityRepository facilityRepository;
    private final ConnectivityDataRepository connectivityDataRepository;
    private final TrafficDataRepository trafficDataRepository;
    private final ReviewRepository reviewRepository;

    private final GooglePlacesService googlePlacesService;
    private final GoogleDistanceMatrixService distanceMatrixService;
    private final GeminiLlmService geminiLlmService;

    private final AppProperties appProperties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Region fetchAndRefreshRegion(double lat, double lng, String geohash, String formattedAddress) {
        Region region = regionRepository.findByGeohash(geohash).orElseGet(() -> {
            Point point = GeometryUtil.createPoint(lat, lng);
            Region newRegion = Region.builder()
                    .geohash(geohash)
                    .centroidLat(lat)
                    .centroidLng(lng)
                    .centroidPoint(point)
                    .formattedAddress(formattedAddress)
                    .build();
            return regionRepository.save(newRegion);
        });

        if (region.getFormattedAddress() == null && formattedAddress != null) {
            region.setFormattedAddress(formattedAddress);
            regionRepository.save(region);
        }

        refreshFacilities(region);
        return region;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshFacilities(Region region) {
        log.info("Refreshing facilities for region: {}", region.getGeohash());

        // Snapshot before wiping — restore if all API calls fail
        List<Facility> existingFacilities = facilityRepository.findByRegionIdOrderByDistance(region.getId());
        facilityRepository.deleteByRegionId(region.getId());

        double lat    = region.getCentroidLat();
        double lng    = region.getCentroidLng();
        int    radius = appProperties.getSearch().getRadiusMeters();

        CompletableFuture<List<Facility>> fSchools   = CompletableFuture.supplyAsync(() -> fetchPlacesList(region, lat, lng, radius, "school", "school", FacilityType.SCHOOL, Integer.MAX_VALUE));
        CompletableFuture<List<Facility>> fHospitals = CompletableFuture.supplyAsync(() -> fetchPlacesList(region, lat, lng, radius, null, "hospital", FacilityType.HOSPITAL, Integer.MAX_VALUE));
        CompletableFuture<List<Facility>> fGroceries = CompletableFuture.supplyAsync(() -> fetchPlacesList(region, lat, lng, radius, null, "grocery_or_supermarket", FacilityType.GROCERY_STORE, Integer.MAX_VALUE));
        CompletableFuture<List<Facility>> fMalls     = CompletableFuture.supplyAsync(() -> fetchPlacesList(region, lat, lng, radius, "mall", "shopping_mall", FacilityType.MALL, Integer.MAX_VALUE));
        CompletableFuture<List<Facility>> fAirports  = CompletableFuture.supplyAsync(() -> fetchPlacesList(region, lat, lng, 50000, "airport", "airport", FacilityType.AIRPORT, 1));
        CompletableFuture<List<Facility>> fSubways   = CompletableFuture.supplyAsync(() -> fetchPlacesList(region, lat, lng, radius, null, "subway_station", FacilityType.METRO_STATION, 2));

        CompletableFuture.allOf(fSchools, fHospitals, fGroceries, fMalls, fAirports, fSubways).join();

        List<Facility> facilities = new ArrayList<>();
        facilities.addAll(fSchools.join());
        facilities.addAll(fHospitals.join());
        facilities.addAll(fGroceries.join());
        facilities.addAll(fMalls.join());
        facilities.addAll(fAirports.join());
        facilities.addAll(fSubways.join());

        if (facilities.isEmpty() && !existingFacilities.isEmpty()) {
            log.warn("All Places API calls returned empty for region {} — restoring previous data", region.getGeohash());
            existingFacilities.forEach(f -> f.setId(null));
            facilityRepository.saveAll(existingFacilities);
            return;
        }

        facilityRepository.saveAll(facilities);
        refreshConnectivity(region, facilities);
        region.setLastUpdated(ZonedDateTime.now());
        region.setAmenitiesScore(ScoringEngine.computeAmenitiesScore(facilities));
        regionRepository.save(region);
    }

    private List<Facility> fetchPlacesList(Region region, double lat, double lng, int radius,
                                           String keyword, String googleType, FacilityType facilityType, int limit) {
        try {
            return googlePlacesService.searchNearby(lat, lng, radius, googleType, keyword).stream()
                    .sorted(Comparator.comparingDouble(p -> HaversineUtil.distanceMeters(lat, lng, p.getLat(), p.getLng())))
                    .limit(limit)
                    .map(p -> Facility.builder()
                            .region(region)
                            .placeId(p.getPlaceId())
                            .name(p.getName())
                            .facilityType(facilityType.name())
                            .lat(p.getLat())
                            .lng(p.getLng())
                            .locationPoint(GeometryUtil.createPoint(p.getLat(), p.getLng()))
                            .rating(p.getRating())
                            .distanceMeters(HaversineUtil.distanceMeters(lat, lng, p.getLat(), p.getLng()))
                            .build())
                    .collect(Collectors.toList());
        } catch (ExternalApiException e) {
            log.warn("Failed to fetch {} places: {}", googleType, e.getMessage());
            return Collections.emptyList();
        }
    }

    private void refreshConnectivity(Region region, List<Facility> facilities) {
        double metroThreshold = appProperties.getThresholds().getMetroMaxDistanceM();

        List<Facility> airports = facilities.stream()
                .filter(f -> FacilityType.AIRPORT.name().equals(f.getFacilityType()))
                .sorted(Comparator.comparingDouble(Facility::getDistanceMeters))
                .limit(1).toList();

        List<Facility> metros = facilities.stream()
                .filter(f -> FacilityType.METRO_STATION.name().equals(f.getFacilityType()))
                .filter(f -> f.getDistanceMeters() != null && f.getDistanceMeters() <= metroThreshold)
                .sorted(Comparator.comparingDouble(Facility::getDistanceMeters))
                .limit(2).toList();

        Region managedRegion = regionRepository.getReferenceById(region.getId());
        ConnectivityData conn = connectivityDataRepository.findByRegionId(region.getId())
                .orElse(ConnectivityData.builder().region(managedRegion).build());

        conn.setNearestAirportName(!airports.isEmpty() ? airports.get(0).getName() : null);
        conn.setNearestAirportDistanceM(!airports.isEmpty() ? airports.get(0).getDistanceMeters() : null);
        conn.setMetroStation1Name(!metros.isEmpty() ? metros.get(0).getName() : null);
        conn.setMetroStation1DistanceM(!metros.isEmpty() ? metros.get(0).getDistanceMeters() : null);
        conn.setMetroStation2Name(metros.size() >= 2 ? metros.get(1).getName() : null);
        conn.setMetroStation2DistanceM(metros.size() >= 2 ? metros.get(1).getDistanceMeters() : null);

        BigDecimal connScore = ScoringEngine.computeConnectivityScore(conn);
        conn.setConnectivityScore(connScore);
        connectivityDataRepository.save(conn);
        region.setConnectivityData(conn);
        region.setConnectivityScore(connScore);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshTraffic(Region region) {
        log.info("Refreshing traffic for region: {}", region.getGeohash());
        try {
            double lat       = region.getCentroidLat();
            double lng       = region.getCentroidLng();
            int    hubRadius = appProperties.getThresholds().getBusinessHubSearchRadiusM();
            double hubLat    = lat;
            double hubLng    = lng;

            try {
                List<PlaceResult> hubs = googlePlacesService.searchNearby(lat, lng, hubRadius, null, "business_park");
                if (hubs.isEmpty()) hubs = googlePlacesService.searchNearby(lat, lng, hubRadius, null, "point_of_interest");
                if (!hubs.isEmpty()) {
                    PlaceResult nearest = hubs.stream()
                            .min(Comparator.comparingDouble(p ->
                                    HaversineUtil.distanceMeters(lat, lng, p.getLat(), p.getLng())))
                            .orElse(hubs.get(0));
                    hubLat = nearest.getLat();
                    hubLng = nearest.getLng();
                    log.info("Dynamic business hub: {} at ({},{})", nearest.getName(), hubLat, hubLng);
                }
            } catch (ExternalApiException e) {
                log.warn("Hub lookup failed for region {}, using centroid: {}", region.getGeohash(), e.getMessage());
            }

            final double fLat = hubLat;
            final double fLng = hubLng;

            // Run Distance Matrix calls in Parallel
            CompletableFuture<DistanceMatrixResult> baselineFuture = CompletableFuture.supplyAsync(() -> 
                distanceMatrixService.getTravelTime(lat, lng, fLat, fLng, false)
            );
            CompletableFuture<DistanceMatrixResult> peakFuture = CompletableFuture.supplyAsync(() -> 
                distanceMatrixService.getTravelTime(lat, lng, fLat, fLng, true)
            );

            DistanceMatrixResult baseline = baselineFuture.join();
            DistanceMatrixResult peak     = peakFuture.join();

            double     multiplier      = baseline.getDurationSeconds() > 0
                    ? (double) peak.getDurationSeconds() / baseline.getDurationSeconds() : 1.0;
            String     congestionLevel = ScoringEngine.classifyCongestion(multiplier);
            BigDecimal trafficScore    = ScoringEngine.computeTrafficScore(congestionLevel);

            Region managedRegion = regionRepository.getReferenceById(region.getId());
            TrafficData traffic = trafficDataRepository.findByRegionId(region.getId())
                    .orElse(TrafficData.builder().region(managedRegion).build());
            traffic.setBaselineTimeSecs(baseline.getDurationSeconds());
            traffic.setPeakTimeSecs(peak.getDurationSeconds());
            traffic.setMultiplier(BigDecimal.valueOf(multiplier).setScale(2, RoundingMode.HALF_UP));
            traffic.setCongestionLevel(congestionLevel);
            traffic.setTrafficScore(trafficScore);
            traffic.setLastTrafficUpdated(ZonedDateTime.now());
            trafficDataRepository.save(traffic);
            region.setTrafficData(traffic);
            region.setTrafficScore(trafficScore);

        } catch (ExternalApiException e) {
            log.warn("Failed to refresh traffic for region {}: {}", region.getGeohash(), e.getMessage());
        }
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshAiSummary(Region region) {
        long reviewCount = reviewRepository.countByRegionId(region.getId());
        int  minReviews  = appProperties.getScoring().getMinReviewsForSentiment();
        if (reviewCount < minReviews) {
            log.info("Insufficient reviews ({}) for AI summary in region {}", reviewCount, region.getGeohash());
            return;
        }

        List<String> reviewTexts = reviewRepository.findByRegionId(region.getId())
                .stream().map(Review::getReviewText).collect(Collectors.toList());

        String regionLabel = region.getFormattedAddress() != null
                ? region.getFormattedAddress()
                : String.format("area near lat=%.4f, lng=%.4f", region.getCentroidLat(), region.getCentroidLng());

        try {
            String summary = geminiLlmService.generateAreaSummary(regionLabel, reviewTexts);
            region.setSummaryText(summary);
            region.setLastSummaryUpdated(ZonedDateTime.now());
            regionRepository.save(region);
            log.info("AI summary saved for region {}", region.getGeohash());
        } catch (ExternalApiException e) {
            log.warn("Failed to generate AI summary for region {}: {}", region.getGeohash(), e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recalculateScores(Region region) {
        Double avgRating   = reviewRepository.averageRatingByRegionId(region.getId());
        long   reviewCount = reviewRepository.countByRegionId(region.getId());

        region.setReviewCount((int) reviewCount);
        if (avgRating != null) {
            BigDecimal avg = BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP);
            region.setAverageRating(avg);
            region.setUserRatingScore(ScoringEngine.computeUserRatingScore(avg));
        }

        // Reload connectivity from DB to catch updates from refreshFacilities
        connectivityDataRepository.findByRegionId(region.getId())
                .ifPresent(conn -> region.setConnectivityScore(conn.getConnectivityScore()));

        AppProperties.Scoring w = appProperties.getScoring();
        region.setLocalityScore(ScoringEngine.computeLocalityScore(
                region.getAmenitiesScore(), region.getConnectivityScore(),
                region.getTrafficScore(),    region.getUserRatingScore(),
                w.getAmenitiesWeight(), w.getConnectivityWeight(),
                w.getTrafficWeight(),   w.getUserRatingWeight()));

        regionRepository.save(region);
        log.info("Scores recalculated for region {}: localityScore={}", region.getGeohash(), region.getLocalityScore());
    }
}