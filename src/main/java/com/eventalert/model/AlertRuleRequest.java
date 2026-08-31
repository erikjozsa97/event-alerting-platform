package com.eventalert.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request to create or update an alert rule. Used for both create and update
 * — PUT replaces the full representation, so there's no separate
 * partial-update shape yet.
 *
 * @param category   which category this rule watches
 * @param name       display name for the rule
 * @param criteria   category-specific matching criteria, validated by the
 *                   matching {@code CriteriaValidator}
 * @param channelIds channels to notify on a match; may be omitted (treated
 *                   as no channels linked)
 * @param active     whether the rule is enabled; may be omitted (defaults to
 *                   {@code true})
 */
public record AlertRuleRequest(
        @NonNull @NotNull Category category,
        @NonNull @NotBlank String name,
        @NonNull @NotNull Map<String, Object> criteria,
        @Nullable List<UUID> channelIds,
        @Nullable Boolean active
) {
}
