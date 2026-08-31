package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EventMatcherDispatcher {

    private final Map<Category, EventMatcher> matchersByCategory;

    public EventMatcherDispatcher(List<EventMatcher> matchers) {
        this.matchersByCategory = matchers.stream()
                .collect(Collectors.toMap(EventMatcher::supports, m -> m));
    }

    public EventMatcher get(Category category) {
        EventMatcher matcher = matchersByCategory.get(category);
        if (matcher == null) {
            throw new IllegalStateException("No EventMatcher registered for category " + category);
        }
        return matcher;
    }
}
