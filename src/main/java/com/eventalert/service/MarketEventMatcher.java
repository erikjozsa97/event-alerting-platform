package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MarketEventMatcher implements EventMatcher {

    @Override
    public Category supports() {
        return Category.MARKET;
    }

    @Override
    public boolean matches(Map<String, Object> criteria, Map<String, Object> eventPayload) {
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
        if (lte instanceof Number lteNum && value > lteNum.doubleValue()) {
            return false;
        }
        return true;
    }
}
