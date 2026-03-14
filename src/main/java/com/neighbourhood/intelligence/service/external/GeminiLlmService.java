package com.neighbourhood.intelligence.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighbourhood.intelligence.config.AppProperties;
import com.neighbourhood.intelligence.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiLlmService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public String generateAreaSummary(String regionName, List<String> reviews) {
        log.info("Generating AI summary from {} reviews", reviews.size());

        String reviewsText = String.join("\n- ", reviews);
        String prompt = """
                You are an AI assistant that summarizes neighborhood reviews.
                Based ONLY on the following user reviews, write a concise 4-5 line factual summary
                of this area. Do not add any information not present in the reviews.
                
                Reviews:
                - %s
                
                Provide only the summary, no preamble.
                """.formatted(reviewsText);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        String url = UriComponentsBuilder.fromHttpUrl(appProperties.getGemini().getBaseUrl())
                .queryParam("key", appProperties.getGemini().getApiKey())
                .toUriString();

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);

            JsonNode response = geminiWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestJson))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(appProperties.getGemini().getApiTimeoutSeconds()))
                    .block();

            if (response == null) {
                throw new ExternalApiException("No response from Gemini API");
            }

            String summary = response
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            if (summary.isBlank()) {
                throw new ExternalApiException("Empty summary from Gemini API");
            }

            log.info("AI summary generated successfully");
            return summary.trim();

        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new ExternalApiException("Error calling Gemini API", e);
        }
    }
}
