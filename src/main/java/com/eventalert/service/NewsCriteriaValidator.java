package com.eventalert.service;

import com.eventalert.exception.InvalidCriteriaException;
import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validator implementation for news alert rule criteria.
 * <p>
 * Validates criteria payloads for the {@link Category#NEWS} category, ensuring that a valid non-empty
 * collection of string keywords is specified and that any provided keyword matching mode
 * ({@code "any"} or {@code "all"}) is supported.
 */
// Expected shape: {"keywords": ["interest rate", "recession"], "match": "any"}
@Component
public class NewsCriteriaValidator implements CriteriaValidator {

    private static final Set<String> VALID_MATCH_MODES = Set.of("any", "all");

    @Override
    @NonNull
    public Category supports() {
        return Category.NEWS;
    }

    @Override
    public void validate(@NonNull Map<String, Object> criteria) {
        Object keywordsRaw = criteria.get("keywords");
        if (!(keywordsRaw instanceof List<?> keywords) || keywords.isEmpty()) {
            throw new InvalidCriteriaException("NEWS criteria requires a non-empty 'keywords' array");
        }
        for (Object keyword : keywords) {
            if (!(keyword instanceof String s) || s.isBlank()) {
                throw new InvalidCriteriaException("Every entry in 'keywords' must be a non-blank string");
            }
        }

        Object match = criteria.get("match");
        if (match != null && !(match instanceof String s && VALID_MATCH_MODES.contains(s))) {
            throw new InvalidCriteriaException("'match' must be one of " + VALID_MATCH_MODES + " when provided");
        }
    }
}
