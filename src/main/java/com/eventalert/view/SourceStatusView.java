package com.eventalert.view;

import com.eventalert.model.Category;

import java.time.OffsetDateTime;

public record SourceStatusView(
        String sourceName,
        Category category,
        boolean configured,
        OffsetDateTime lastPolledAt,
        int lastEventCount,
        String lastError
) {
}
