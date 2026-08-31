package com.eventalert.view;

import com.eventalert.model.AlertRule;
import com.eventalert.model.Category;

import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What actually leaves the API for an {@link AlertRule}, scoped to its owner
 * (no owner id/email — the caller already knows it's their own rule).
 */
public record AlertRuleView(
        @NonNull UUID id,
        @NonNull Category category,
        @NonNull String name,
        @NonNull Map<String, Object> criteria,
        boolean active,
        @NonNull OffsetDateTime createdAt,
        @NonNull List<UUID> channelIds
) {
    /**
     * Builds the owner-facing view of an alert rule and its linked channel ids.
     */
    @NonNull
    public static AlertRuleView from(@NonNull AlertRule rule, @NonNull List<UUID> channelIds) {
        return new AlertRuleView(
                rule.getId(),
                rule.getCategory(),
                rule.getName(),
                rule.getCriteria(),
                rule.isActive(),
                rule.getCreatedAt(),
                channelIds
        );
    }
}
