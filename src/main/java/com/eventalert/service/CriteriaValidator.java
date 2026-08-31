package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Validates an alert rule's criteria for its category — one implementation
 * per {@link Category}.
 */
public interface CriteriaValidator {

    @NonNull
    Category supports();

    /**
     * @throws com.eventalert.exception.InvalidCriteriaException if criteria is malformed for this category
     */
    void validate(@NonNull Map<String, Object> criteria);
}
