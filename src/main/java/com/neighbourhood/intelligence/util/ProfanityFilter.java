package com.neighbourhood.intelligence.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ProfanityFilter {

    private final List<String> profanityList;

    public ProfanityFilter(
            @Value("${app.profanity.words:badword1,badword2,offensive1,offensive2}")
            List<String> profanityList) {
        this.profanityList = profanityList;
        log.info("ProfanityFilter loaded with {} words", profanityList.size());
    }

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) return false;
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        for (String word : profanityList) {
            if (normalized.contains(word.toLowerCase())) {
                log.debug("Profanity detected in review text");
                return true;
            }
        }
        return false;
    }

    public String sanitize(String text) {
        if (text == null) return null;
        String result = text;
        for (String word : profanityList) {
            result = result.replaceAll(
                    "(?i)\\b" + Pattern.quote(word) + "\\b",
                    "*".repeat(word.length()));
        }
        return result;
    }
}