package com.neighbourhood.intelligence.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupValidator {

    private final AppProperties appProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validateRequiredConfig() {
        List<String> missing = new ArrayList<>();

        if (isBlank(appProperties.getGoogle().getGeocodingApiKey()))     missing.add("GOOGLE_GEOCODING_API_KEY");
        if (isBlank(appProperties.getGoogle().getPlacesApiKey()))         missing.add("GOOGLE_PLACES_API_KEY");
        if (isBlank(appProperties.getGoogle().getDistanceMatrixApiKey())) missing.add("GOOGLE_DISTANCE_MATRIX_API_KEY");
        if (isBlank(appProperties.getGemini().getApiKey()))               missing.add("GEMINI_API_KEY");
        if (isBlank(appProperties.getSecurity().getGoogleClientId()))     missing.add("GOOGLE_CLIENT_ID");
        if (isBlank(environment.getProperty("spring.data.redis.host")))   missing.add("REDIS_HOST");

        if (!missing.isEmpty()) {
            String msg = "STARTUP FAILED — missing required environment variables: " + missing;
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info("Startup validation passed — all required config present");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}