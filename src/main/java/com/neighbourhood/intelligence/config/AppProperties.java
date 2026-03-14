package com.neighbourhood.intelligence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Google google = new Google();
    private Gemini gemini = new Gemini();
    private Cache cache = new Cache();
    private Search search = new Search();
    private Scoring scoring = new Scoring();
    private Thresholds thresholds = new Thresholds();
    private Profanity profanity = new Profanity();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();
    private Security security = new Security();

    @Data
    public static class Google {
        private String geocodingApiKey;
        private String placesApiKey;
        private String distanceMatrixApiKey;
        private String geocodingBaseUrl;
        private String placesBaseUrl;
        private String distanceMatrixBaseUrl;
        private int apiTimeoutSeconds = 10;
    }

    @Data
    public static class Gemini {
        private String apiKey;
        private String baseUrl;
        private int apiTimeoutSeconds = 30;
    }

    @Data
    public static class Cache {
        private int regionTtlDays = 30;
        private int trafficTtlDays = 180;
        private int summaryTtlDays = 30;
    }

    @Data
    public static class Search {
        private int radiusMeters = 5000;
        private int geohashPrecision = 6;
    }

    @Data
    public static class Scoring {
        private int minReviewsForSentiment = 5;
        private double amenitiesWeight = 0.30;
        private double connectivityWeight = 0.25;
        private double trafficWeight = 0.20;
        private double userRatingWeight = 0.25;
    }

    @Data
    public static class Thresholds {
        private double metroMaxDistanceM = 30_000;
        private double mallMaxDistanceM = 40_000;
        private int businessHubSearchRadiusM = 25_000;
    }

    @Data
    public static class Profanity {
        private List<String> words = List.of();
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    @Data
    public static class RateLimit {
        private int searchRequestsPerMinute = 30;
        private int reviewRequestsPerMinute = 10;
    }

    @Data
    public static class Security {
        private String googleClientId;
    }
}