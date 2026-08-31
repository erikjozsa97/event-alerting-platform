package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event matcher implementation for evaluating disaster event payloads against alert rule criteria.
 * <p>
 * Matches incoming disaster events (e.g., earthquakes) based on minimum magnitude thresholds
 * and optional geographic distance constraints calculated using the Haversine formula.
 */
@Component
public class DisasterEventMatcher implements EventMatcher {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    @NonNull
    public Category supports() {
        return Category.DISASTER;
    }

    @Override
    public boolean matches(@NonNull Map<String, Object> criteria, @NonNull  Map<String, Object> eventPayload) {
        if (!(criteria.get("minMagnitude") instanceof Number minMagnitude)
                || !(eventPayload.get("magnitude") instanceof Number eventMagnitude)) {
            return false;
        }
        if (eventMagnitude.doubleValue() < minMagnitude.doubleValue()) {
            return false;
        }

        Object regionRaw = criteria.get("region");
        if (regionRaw == null) {
            return true; // no region filter — magnitude alone decides
        }
        if (!(regionRaw instanceof Map<?, ?> region)) {
            return false;
        }

        if (!(eventPayload.get("lat") instanceof Number eventLat)
                || !(eventPayload.get("lon") instanceof Number eventLon)) {
            return false;
        }
        if (!(region.get("lat") instanceof Number regionLat)
                || !(region.get("lon") instanceof Number regionLon)
                || !(region.get("radiusKm") instanceof Number radiusKm)) {
            return false;
        }

        double distanceKm = haversineKm(regionLat.doubleValue(), regionLon.doubleValue(),
                eventLat.doubleValue(), eventLon.doubleValue());
        return distanceKm <= radiusKm.doubleValue();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
