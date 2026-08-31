package com.eventalert.service;

import com.eventalert.exception.InvalidCriteriaException;
import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

// Expected shape: {"minMagnitude": 6.0, "region": {"lat": 37.77, "lon": -122.41, "radiusKm": 500}}
/**
 * Validator implementation for disaster alert rule criteria.
 * <p>
 * Validates criteria payloads for the {@link Category#DISASTER} category, ensuring required
 * fields such as {@code minMagnitude} fall within valid bounds (0.0 to 10.0) and optional geographic
 * boundary parameters ({@code lat}, {@code lon}, {@code radiusKm}) contain numeric values.
 */
@Component
public class DisasterCriteriaValidator implements CriteriaValidator {

    @Override
    @NonNull
    public Category supports() {
        return Category.DISASTER;
    }

    @Override
    public void validate(@NonNull Map<String, Object> criteria) {
        Object minMagnitudeRaw = criteria.get("minMagnitude");
        if (!(minMagnitudeRaw instanceof Number minMagnitude)) {
            throw new InvalidCriteriaException("DISASTER criteria requires a numeric 'minMagnitude'");
        }
        double value = minMagnitude.doubleValue();
        if (value < 0 || value > 10) {
            throw new InvalidCriteriaException("'minMagnitude' must be between 0 and 10");
        }

        Object regionRaw = criteria.get("region");
        if (regionRaw != null) {
            if (!(regionRaw instanceof Map<?, ?> region)) {
                throw new InvalidCriteriaException("'region' must be an object with 'lat', 'lon', 'radiusKm'");
            }
            requireNumber(region, "lat");
            requireNumber(region, "lon");
            requireNumber(region, "radiusKm");
        }
    }

    private void requireNumber(@NonNull Map<?, ?> region, String field) {
        if (!(region.get(field) instanceof Number)) {
            throw new InvalidCriteriaException("'region." + field + "' must be a number");
        }
    }
}
