package com.neighbourhood.intelligence.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.neighbourhood.intelligence.config.AppProperties;
import com.neighbourhood.intelligence.exception.ExternalApiException;
import com.neighbourhood.intelligence.service.external.model.DistanceMatrixResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleDistanceMatrixService {

    @Qualifier("googleWebClient")
    private final WebClient googleWebClient;
    private final AppProperties appProperties;

    public DistanceMatrixResult getTravelTime(double originLat, double originLng,
                                              double destLat, double destLng,
                                              boolean isPeak) {
        log.info("Fetching travel time: isPeak={}", isPeak);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(appProperties.getGoogle().getDistanceMatrixBaseUrl())
                .queryParam("origins", originLat + "," + originLng)
                .queryParam("destinations", destLat + "," + destLng)
                .queryParam("key", appProperties.getGoogle().getDistanceMatrixApiKey());

        if (isPeak) {
            // Set departure time to next 9AM (Monday for reliability)
            long nextPeakEpoch = getNextWeekdayHourEpoch(9);
            builder.queryParam("departure_time", nextPeakEpoch);
            builder.queryParam("traffic_model", "pessimistic");
        } else {
            // Baseline: next 3AM
            long nextBaselineEpoch = getNextWeekdayHourEpoch(3);
            builder.queryParam("departure_time", nextBaselineEpoch);
        }

        String url = builder.toUriString();

        try {
            JsonNode response = googleWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(appProperties.getGoogle().getApiTimeoutSeconds()))
                    .block();

            if (response == null || !"OK".equals(response.path("status").asText())) {
                throw new ExternalApiException("Distance Matrix API returned non-OK status");
            }

            JsonNode element = response.path("rows").get(0)
                    .path("elements").get(0);

            String elementStatus = element.path("status").asText();
            if (!"OK".equals(elementStatus)) {
                throw new ExternalApiException("Distance Matrix element status: " + elementStatus);
            }

            int durationSecs = isPeak
                    ? element.path("duration_in_traffic").path("value").asInt()
                    : element.path("duration").path("value").asInt();
            int distanceMeters = element.path("distance").path("value").asInt();

            return DistanceMatrixResult.builder()
                    .durationSeconds(durationSecs)
                    .distanceMeters(distanceMeters)
                    .build();

        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Distance Matrix API: {}", e.getMessage(), e);
            throw new ExternalApiException("Error calling Distance Matrix API", e);
        }
    }

    private long getNextWeekdayHourEpoch(int hour) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime target = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (target.isBefore(now)) {
            target = target.plusDays(1);
        }
        // Skip weekends for traffic baseline
        while (target.getDayOfWeek().getValue() >= 6) {
            target = target.plusDays(1);
        }
        return target.toEpochSecond(ZoneOffset.UTC);
    }
}
