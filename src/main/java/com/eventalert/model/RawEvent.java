package com.eventalert.model;

import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * What an {@code EventSource} hands back before it has a database id — the
 * repository assigns one on insert.
 *
 * @param source     short identifier of where this came from (e.g. "usgs")
 * @param externalId the source's own id for this event, used for dedup
 * @param category   which alert category this event belongs to
 * @param payload    normalized event data, shape depends on category
 * @param occurredAt when the underlying real-world event happened
 */
public record RawEvent(
        @NonNull String source,
        @NonNull String externalId,
        @NonNull Category category,
        @NonNull Map<String, Object> payload,
        @NonNull OffsetDateTime occurredAt
) {
}
