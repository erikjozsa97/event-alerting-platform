package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dispatcher component that resolves the appropriate {@link EventMatcher} for a given event category.
 * <p>
 * Collects all available {@link EventMatcher} strategy implementations spring-managed in the application
 * context and maps them by their supported {@link Category}.
 */
@Component
public class EventMatcherDispatcher {

    private final Map<Category, EventMatcher> matchersByCategory;

    public EventMatcherDispatcher(@NonNull List<EventMatcher> matchers) {
        this.matchersByCategory = matchers.stream()
                .collect(Collectors.toMap(EventMatcher::supports, m -> m));
    }

    @NonNull
    public EventMatcher get(@NonNull Category category) {
        EventMatcher matcher = matchersByCategory.get(category);
        if (matcher == null) {
            throw new IllegalStateException("No EventMatcher registered for category " + category);
        }
        return matcher;
    }
}
