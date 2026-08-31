package com.eventalert.service;

import com.eventalert.exception.InvalidCriteriaException;
import com.eventalert.model.Category;
import org.springframework.stereotype.Component;

import java.util.Map;

// Expected shape: {"symbol": "AAPL", "changePercent": {"gte": 5}}
@Component
public class MarketCriteriaValidator implements CriteriaValidator {

    @Override
    public Category supports() {
        return Category.MARKET;
    }

    @Override
    public void validate(Map<String, Object> criteria) {
        Object symbolRaw = criteria.get("symbol");
        if (!(symbolRaw instanceof String symbol) || symbol.isBlank()) {
            throw new InvalidCriteriaException("MARKET criteria requires a non-blank 'symbol' string");
        }

        Object changePercentRaw = criteria.get("changePercent");
        if (!(changePercentRaw instanceof Map<?, ?> changePercent) || changePercent.isEmpty()) {
            throw new InvalidCriteriaException(
                    "MARKET criteria requires a 'changePercent' object with 'gte' and/or 'lte'");
        }

        Object gte = changePercent.get("gte");
        Object lte = changePercent.get("lte");
        if (gte == null && lte == null) {
            throw new InvalidCriteriaException("'changePercent' must define at least one of 'gte' or 'lte'");
        }
        if (gte != null && !(gte instanceof Number)) {
            throw new InvalidCriteriaException("'changePercent.gte' must be a number");
        }
        if (lte != null && !(lte instanceof Number)) {
            throw new InvalidCriteriaException("'changePercent.lte' must be a number");
        }
    }
}
