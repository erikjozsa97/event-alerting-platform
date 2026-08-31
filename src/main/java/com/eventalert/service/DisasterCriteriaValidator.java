package com.eventalert.service;

import com.eventalert.exception.InvalidCriteriaException;
import com.eventalert.model.Category;
import org.springframework.stereotype.Component;

import java.util.Map;

// Expected shape: {"minMagnitude": 6.0, "region": {"lat": 37.77, "lon": -122.41, "radiusKm": 500}}
@Component
public class DisasterCriteriaValidator implements CriteriaValidator {

    @Override
    public Category supports() {
        return Category.DISASTER;
    }

    @Override
    public void validate(Map<String, Object> criteria) {
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

    private void requireNumber(Map<?, ?> region, String field) {
        if (!(region.get(field) instanceof Number)) {
            throw new InvalidCriteriaException("'region." + field + "' must be a number");
        }
    }
}
