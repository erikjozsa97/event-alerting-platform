package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Matches an ingested event's payload against a rule's criteria — one
 * implementation per {@link Category}, mirroring {@link CriteriaValidator}.
 */
public interface EventMatcher {

    @NonNull
    Category supports();

    boolean matches(@NonNull Map<String, Object> criteria, @NonNull Map<String, Object> eventPayload);
}
