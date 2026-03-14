package com.neighbourhood.intelligence.service.impl;

import com.neighbourhood.intelligence.config.AppProperties;
import com.neighbourhood.intelligence.entity.Region;
import com.neighbourhood.intelligence.repository.RegionRepository;
import com.neighbourhood.intelligence.service.RegionDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskService {

    private static final int BATCH_SIZE = 50;

    private final RegionRepository regionRepository;
    private final RegionDataService regionDataService;
    private final AppProperties appProperties;

    // 1st of every month at 2AM
    @Scheduled(cron = "0 0 2 1 * *")
    public void refreshAiSummariesMonthly() {
        log.info("Scheduled job: starting monthly AI summary refresh");
        int summaryTtlDays = appProperties.getCache().getSummaryTtlDays();
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(summaryTtlDays);

        int pageNum = 0;
        Page<Region> page;

        do {
            page = regionRepository.findRegionsWithStaleSummary(
                    cutoff, PageRequest.of(pageNum, BATCH_SIZE));

            log.info("Processing summary batch {}/{} ({} regions)",
                    pageNum + 1, page.getTotalPages(), page.getNumberOfElements());

            for (Region region : page.getContent()) {
                try {
                    regionDataService.refreshAiSummary(region);
                } catch (Exception e) {
                    log.error("Failed to refresh AI summary for region {}: {}",
                            region.getGeohash(), e.getMessage());
                }
            }
            pageNum++;
        } while (!page.isLast());

        log.info("Scheduled job: monthly AI summary refresh complete");
    }

    // Jan 1 and Jul 1 at 3AM
    @Scheduled(cron = "0 0 3 1 1,7 *")
    public void refreshTrafficBiannually() {
        log.info("Scheduled job: starting biannual traffic refresh");
        int trafficTtlDays = appProperties.getCache().getTrafficTtlDays();
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(trafficTtlDays);

        int pageNum = 0;
        Page<Region> page;

        do {
            page = regionRepository.findRegionsWithStaleTraffic(
                    cutoff, PageRequest.of(pageNum, BATCH_SIZE));

            log.info("Processing traffic batch {}/{} ({} regions)",
                    pageNum + 1, page.getTotalPages(), page.getNumberOfElements());

            for (Region region : page.getContent()) {
                try {
                    regionDataService.refreshTraffic(region);
                } catch (Exception e) {
                    log.error("Failed to refresh traffic for region {}: {}",
                            region.getGeohash(), e.getMessage());
                }
            }
            pageNum++;
        } while (!page.isLast());

        log.info("Scheduled job: biannual traffic refresh complete");
    }

    // Every day at 1AM
    @Scheduled(cron = "0 0 1 * * *")
    public void refreshStaleFacilitiesDaily() {
        log.info("Scheduled job: checking for stale facility data");
        int regionTtlDays = appProperties.getCache().getRegionTtlDays();
        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(regionTtlDays);

        int pageNum = 0;
        Page<Region> page;

        do {
            page = regionRepository.findStaleRegions(
                    cutoff, PageRequest.of(pageNum, BATCH_SIZE));

            log.info("Processing facility batch {}/{} ({} regions)",
                    pageNum + 1, page.getTotalPages(), page.getNumberOfElements());

            for (Region region : page.getContent()) {
                try {
                    regionDataService.refreshFacilities(region);
                    regionDataService.recalculateScores(region);
                } catch (Exception e) {
                    log.error("Failed to refresh facilities for region {}: {}",
                            region.getGeohash(), e.getMessage());
                }
            }
            pageNum++;
        } while (!page.isLast());

        log.info("Scheduled job: stale facility refresh complete");
    }
}