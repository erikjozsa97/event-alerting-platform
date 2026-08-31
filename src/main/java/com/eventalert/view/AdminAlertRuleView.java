package com.eventalert.view;

import com.eventalert.model.AlertRule;
import com.eventalert.model.Category;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminAlertRuleView(
        UUID id,
        UUID userId,
        String userEmail,
        Category category,
        String name,
        Map<String, Object> criteria,
        boolean active,
        OffsetDateTime createdAt,
        List<UUID> channelIds
) {
    public static AdminAlertRuleView from(AlertRule rule, String userEmail, List<UUID> channelIds) {
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
