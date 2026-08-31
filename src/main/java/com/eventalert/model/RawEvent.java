package com.eventalert.model;

import java.time.OffsetDateTime;
import java.util.Map;

public record RawEvent(
        String source,
        String externalId,
        Category category,
        Map<String, Object> payload,
        OffsetDateTime occurredAt
) {
}
