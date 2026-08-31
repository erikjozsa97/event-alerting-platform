package com.eventalert.service;

import com.eventalert.exception.InvalidCriteriaException;
import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validator implementation for financial market alert rule criteria.
 * <p>
 * Validates criteria payloads for the {@link Category#MARKET} category, ensuring that a valid
 * stock or asset ticker symbol is present and that percentage change bounds ({@code gte} and/or {@code lte})
 * are properly structured and numeric.
 */
// Expected shape: {"symbol": "AAPL", "changePercent": {"gte": 5}}
@Component
public class MarketCriteriaValidator implements CriteriaValidator {

    @Override
    @NonNull
    public Category supports() {
        return Category.MARKET;
    }

    @Override
    public void validate(@NonNull Map<String, Object> criteria) {
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
