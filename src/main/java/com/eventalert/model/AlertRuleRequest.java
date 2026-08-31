package com.eventalert.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// Used for both create and update — PUT replaces the full representation,
// so there's no separate partial-update shape yet.
public record AlertRuleRequest(
        @NotNull Category category,
        @NotBlank String name,
        @NotNull Map<String, Object> criteria,
        List<UUID> channelIds,
        Boolean active
) {
}
