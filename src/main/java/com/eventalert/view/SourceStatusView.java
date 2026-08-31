package com.eventalert.view;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

/**
 * Admin-only view of an {@code EventSource}'s current health, tracked
 * in-memory by {@code IngestionScheduler}.
 *
 * @param sourceName    short identifier of the source (e.g. "usgs")
 * @param category      which alert category this source feeds
 * @param configured    whether the source has what it needs to fetch (e.g. an API key)
 * @param lastPolledAt  when this source was last polled; {@code null} before the first poll
 * @param lastEventCount how many new events the last poll produced
 * @param lastError     the last poll's error message, if any; {@code null} otherwise
 */
public record SourceStatusView(
        @NonNull String sourceName,
        @NonNull Category category,
        boolean configured,
        @Nullable OffsetDateTime lastPolledAt,
        int lastEventCount,
        @Nullable String lastError
) {
}
