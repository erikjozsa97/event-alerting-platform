package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Event matcher implementation for evaluating news event payloads against alert rule criteria.
 * <p>
 * Matches incoming news articles by scanning combined title, description, and content text
 * against a list of target keywords using case-insensitive search logic for either all-match
 * or any-match modes.
 */
@Component
public class NewsEventMatcher implements EventMatcher {

    @Override
    @NonNull
    public Category supports() {
        return Category.NEWS;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(@NonNull Map<String, Object> criteria, @NonNull Map<String, Object> eventPayload) {
        Object keywordsRaw = criteria.get("keywords");
        if (!(keywordsRaw instanceof List<?> keywords) || keywords.isEmpty()) {
            return false;
        }

        String haystack = String.join(" ",
                String.valueOf(eventPayload.getOrDefault("title", "")),
                String.valueOf(eventPayload.getOrDefault("description", "")),
                String.valueOf(eventPayload.getOrDefault("content", ""))
        ).toLowerCase();

        boolean matchAll = "all".equals(criteria.get("match"));
        List<String> keywordStrings = (List<String>) keywords;

        return matchAll
                ? keywordStrings.stream().allMatch(k -> haystack.contains(k.toLowerCase()))
                : keywordStrings.stream().anyMatch(k -> haystack.contains(k.toLowerCase()));
    }
}
