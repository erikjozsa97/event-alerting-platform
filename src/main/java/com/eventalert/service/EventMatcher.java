package com.eventalert.service;

import com.eventalert.model.Category;

import java.util.Map;

public interface EventMatcher {

    Category supports();

    boolean matches(Map<String, Object> criteria, Map<String, Object> eventPayload);
}
