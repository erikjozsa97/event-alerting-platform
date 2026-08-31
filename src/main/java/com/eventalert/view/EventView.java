package com.eventalert.view;

import com.eventalert.model.Category;
import com.eventalert.model.Event;
import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only view of a raw ingested {@link Event}.
 */
public record EventView(
        @NonNull UUID id,
        @NonNull String source,
        @NonNull String externalId,
        @NonNull Category category,
        @NonNull Map<String, Object> payload,
        @NonNull OffsetDateTime occurredAt,
        @NonNull OffsetDateTime ingestedAt
) {
    /**
     * Builds the admin-facing view of an ingested event.
     */
    @NonNull
    public static EventView from(@NonNull Event event) {
        return new EventView(
                event.getId(),
                event.getSource(),
                event.getExternalId(),
                event.getCategory(),
                event.getPayload(),
                event.getOccurredAt(),
                event.getIngestedAt()
        );
    }
}
