package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event matcher implementation for evaluating market event payloads against alert rule criteria.
 * <p>
 * Matches incoming market events (e.g., stock price updates) by checking case-insensitive ticker symbol equality
 * and validating that event percentage changes fall within specified numeric bounds ({@code gte} and/or {@code lte}).
 */
@Component
public class MarketEventMatcher implements EventMatcher {

    @Override
    @NonNull
    public Category supports() {
        return Category.MARKET;
    }

    @Override
    public boolean matches(@NonNull Map<String, Object> criteria, @NonNull Map<String, Object> eventPayload) {
        Object criteriaSymbol = criteria.get("symbol");
        Object eventSymbol = eventPayload.get("symbol");
        if (criteriaSymbol == null || !criteriaSymbol.toString().equalsIgnoreCase(String.valueOf(eventSymbol))) {
            return false;
        }

        if (!(eventPayload.get("changePercent") instanceof Number changePercent)) {
            return false;
        }
        if (!(criteria.get("changePercent") instanceof Map<?, ?> threshold)) {
            return false;
        }

        double value = changePercent.doubleValue();
        Object gte = threshold.get("gte");
        Object lte = threshold.get("lte");

        if (gte == null && lte == null) {
            return false;
        }
        if (gte instanceof Number gteNum && value < gteNum.doubleValue()) {
            return false;
        }
        return !(lte instanceof Number lteNum) || !(value > lteNum.doubleValue());
    }
}
