package com.eventalert.service;

import com.eventalert.model.Category;

import java.util.Map;

public interface CriteriaValidator {

    Category supports();

    /**
     * @throws com.eventalert.exception.InvalidCriteriaException if criteria is malformed for this category
     */
    void validate(Map<String, Object> criteria);
}
