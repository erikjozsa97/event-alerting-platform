package com.eventalert.view;

import com.eventalert.model.Category;
import com.eventalert.model.Event;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record EventView(
        UUID id,
        String source,
        String externalId,
        Category category,
        Map<String, Object> payload,
        OffsetDateTime occurredAt,
        OffsetDateTime ingestedAt
) {
    public static EventView from(Event event) {
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
