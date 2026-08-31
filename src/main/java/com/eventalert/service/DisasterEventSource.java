package com.eventalert.service;

import com.eventalert.model.Category;
import com.eventalert.model.RawEvent;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Event source implementation for fetching real-time seismic disaster data from the USGS GeoJSON feed.
 * <p>
 * Polling client that retrieves recent earthquake events, parses GeoJSON features, and maps them
 * into standardized {@link RawEvent} instances for upstream processing.
 */
// USGS's real-time earthquake feed: free, no API key, stable GeoJSON schema.
// https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php
@Component
public class DisasterEventSource implements EventSource {

    private static final String FEED_URL =
            "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_hour.geojson";

    private final RestClient restClient = RestClient.create();

    @Override
    @NonNull
    public Category getCategory() {
        return Category.DISASTER;
    }

    @Override
    @NonNull
    public String getSourceName() {
        return "usgs";
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public List<RawEvent> fetchLatest() {
        Map<String, Object> response;
        try {
            response = restClient.get().uri(FEED_URL).retrieve().body(Map.class);
        } catch (Exception e) {
            return List.of(); // feed unreachable this cycle — try again next poll
        }
        if (response == null) {
            return List.of();
        }

        Object featuresRaw = response.get("features");
        if (!(featuresRaw instanceof List<?> features)) {
            return List.of();
        }

        List<RawEvent> events = new ArrayList<>();
        for (Object featureObj : features) {
            if (!(featureObj instanceof Map<?, ?> feature)) {
                continue;
            }
            RawEvent event = toRawEvent((Map<String, Object>) feature);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private RawEvent toRawEvent(@NonNull  Map<String, Object> feature) {
        Object propertiesRaw = feature.get("properties");
        Object geometryRaw = feature.get("geometry");
        Object idRaw = feature.get("id");
        if (!(propertiesRaw instanceof Map<?, ?> properties) || !(geometryRaw instanceof Map<?, ?> geometry)
                || idRaw == null) {
            return null;
        }

        Object magnitude = properties.get("mag");
        Object timeRaw = properties.get("time");
        long timeMillis = timeRaw instanceof Number n ? n.longValue() : 0L;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("magnitude", magnitude);
        payload.put("place", properties.get("place"));
        payload.put("title", properties.get("title"));
        payload.put("url", properties.get("url"));

        Object coordinatesRaw = geometry.get("coordinates");
        if (coordinatesRaw instanceof List<?> coordinates && coordinates.size() >= 2) {
            payload.put("lon", coordinates.get(0));
            payload.put("lat", coordinates.get(1));
            if (coordinates.size() >= 3) {
                payload.put("depthKm", coordinates.get(2));
            }
        }

        return new RawEvent(
                getSourceName(),
                String.valueOf(idRaw),
                Category.DISASTER,
                payload,
                Instant.ofEpochMilli(timeMillis).atOffset(ZoneOffset.UTC)
        );
    }
}
