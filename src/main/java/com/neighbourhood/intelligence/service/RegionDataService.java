package com.neighbourhood.intelligence.service;

import com.neighbourhood.intelligence.entity.Region;

public interface RegionDataService {
    Region fetchAndRefreshRegion(double lat, double lng, String geohash, String formattedAddress);
    void refreshFacilities(Region region);
    void refreshTraffic(Region region);
    void refreshAiSummary(Region region);
    void recalculateScores(Region region);
}