package com.neighbourhood.intelligence.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.neighbourhood.intelligence.config.AppProperties;
import com.neighbourhood.intelligence.exception.ExternalApiException;
import com.neighbourhood.intelligence.service.external.model.PlaceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GooglePlacesService {

    @Qualifier("googleWebClient")
    private final WebClient googleWebClient;
    private final AppProperties appProperties;

    public List<PlaceResult> searchNearby(double lat, double lng, int radiusMeters, String type, String keyword) {
        log.info("Searching nearby places: type={}, keyword={}, lat={}, lng={}, radius={}", type, keyword, lat, lng, radiusMeters);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(appProperties.getGoogle().getPlacesBaseUrl())
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", radiusMeters)
                .queryParam("type", type)
                .queryParam("key", appProperties.getGoogle().getPlacesApiKey());

        if (keyword != null && !keyword.isBlank()) {
            uriBuilder.queryParam("keyword", keyword);
        }

        String url = uriBuilder.toUriString();
        try {
            JsonNode response = googleWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(appProperties.getGoogle().getApiTimeoutSeconds()))
                    .block();

            if (response == null) {
                throw new ExternalApiException("No response from Places API");
            }

            String status = response.path("status").asText();
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                throw new ExternalApiException("Places API returned status: " + status);
            }

            List<PlaceResult> results = new ArrayList<>();
            JsonNode resultsArray = response.path("results");
            for (JsonNode result : resultsArray) {
                PlaceResult place = PlaceResult.builder()
                        .placeId(result.path("place_id").asText(null))
                        .name(result.path("name").asText())
                        .lat(result.path("geometry").path("location").path("lat").asDouble())
                        .lng(result.path("geometry").path("location").path("lng").asDouble())
                        .rating(result.has("rating")
                                ? BigDecimal.valueOf(result.path("rating").asDouble())
                                : null)
                        .build();
                results.add(place);
            }

            log.info("Found {} places of type {}", results.size(), type);
            return results;

        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Places API: {}", e.getMessage(), e);
            throw new ExternalApiException("Error calling Places API", e);
        }
    }
}
