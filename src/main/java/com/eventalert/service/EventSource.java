package com.eventalert.service;

import com.eventalert.model.Category;
import com.eventalert.model.RawEvent;

import java.util.List;

public interface EventSource {

    Category getCategory();

    String getSourceName();

    /**
     * Should never throw for "no new data" or "provider unavailable" — return an
     * empty list instead so one flaky source doesn't stop the others from polling.
     */
    List<RawEvent> fetchLatest();

    /**
     * True if this source has what it needs to actually fetch (e.g. an API key).
     * Sources that need no configuration (USGS) just use the default.
     */
    default boolean isConfigured() {
        return true;
    }
}
