package com.eventalert.view;

import com.eventalert.model.AlertRule;
import com.eventalert.model.Category;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AlertRuleView(
        UUID id,
        Category category,
        String name,
        Map<String, Object> criteria,
        boolean active,
        OffsetDateTime createdAt,
        List<UUID> channelIds
) {
    public static AlertRuleView from(AlertRule rule, List<UUID> channelIds) {
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
