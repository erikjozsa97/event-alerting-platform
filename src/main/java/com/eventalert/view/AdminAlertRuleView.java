package com.eventalert.view;

import com.eventalert.model.AlertRule;
import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only view of an alert rule — unlike {@link AlertRuleView}, includes
 * the owning user so an admin can see whose rule it is.
 */
public record AdminAlertRuleView(
        @NonNull UUID id,
        @NonNull UUID userId,
        @Nullable String userEmail,
        @NonNull Category category,
        @NonNull String name,
        @NonNull Map<String, Object> criteria,
        boolean active,
        @NonNull OffsetDateTime createdAt,
        @NonNull List<UUID> channelIds
) {
    /**
     * Builds the admin-facing view of an alert rule, attaching the owner's email.
     */
    @NonNull
    public static AdminAlertRuleView from(@NonNull AlertRule rule, @Nullable String userEmail,
                                           @NonNull List<UUID> channelIds) {
        return new AdminAlertRuleView(
                rule.getId(),
                rule.getUserId(),
                userEmail,
                rule.getCategory(),
                rule.getName(),
                rule.getCriteria(),
                rule.isActive(),
                rule.getCreatedAt(),
                channelIds
        );
    }
}
