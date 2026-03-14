package com.neighbourhood.intelligence.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.neighbourhood.intelligence.config.AppProperties;
import com.neighbourhood.intelligence.entity.GeocodingCache;
import com.neighbourhood.intelligence.exception.ExternalApiException;
import com.neighbourhood.intelligence.repository.GeocodingCacheRepository;
import com.neighbourhood.intelligence.service.external.model.GeocodingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleGeocodingService {

    @Qualifier("googleWebClient")
    private final WebClient googleWebClient;
    private final AppProperties appProperties;
    private final GeocodingCacheRepository geocodingCacheRepository;

    public GeocodingResult geocode(String address) {
        log.info("Geocoding address: {}", address);

        String queryKey = address.trim().toLowerCase();

        // Check DB cache first
        Optional<GeocodingCache> cached = geocodingCacheRepository.findByQueryKey(queryKey);
        if (cached.isPresent()) {
            GeocodingCache hit = cached.get();
            log.info("Geocoding cache hit for query: {}", address);
            return GeocodingResult.builder()
                    .lat(hit.getLat())
                    .lng(hit.getLng())
                    .formattedAddress(hit.getFormattedAddress())
                    .build();
        }

        log.info("Geocoding cache miss, calling Google API for: {}", address);

        String url = appProperties.getGoogle().getGeocodingBaseUrl()
                + "?address=" + address.replace(" ", "+")
                + "&key=" + appProperties.getGoogle().getGeocodingApiKey();

        try {
            JsonNode response = googleWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(appProperties.getGoogle().getApiTimeoutSeconds()))
                    .block();

            if (response == null || !"OK".equals(response.path("status").asText())) {
                String status = response != null ? response.path("status").asText() : "NULL_RESPONSE";
                log.error("Geocoding API returned status: {}", status);
                throw new ExternalApiException("Geocoding failed with status: " + status);
            }

            JsonNode location = response.path("results").get(0)
                    .path("geometry").path("location");
            String formattedAddress = response.path("results").get(0)
                    .path("formatted_address").asText();

            double lat = location.path("lat").asDouble();
            double lng = location.path("lng").asDouble();

            // Save to cache
            geocodingCacheRepository.save(GeocodingCache.builder()
                    .queryKey(queryKey)
                    .lat(lat)
                    .lng(lng)
                    .formattedAddress(formattedAddress)
                    .build());
            log.info("Geocoding result cached for query: {}", address);

            return GeocodingResult.builder()
                    .lat(lat)
                    .lng(lng)
                    .formattedAddress(formattedAddress)
                    .build();

        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Geocoding API: {}", e.getMessage(), e);
            throw new ExternalApiException("Error calling Geocoding API", e);
        }
    }
}
