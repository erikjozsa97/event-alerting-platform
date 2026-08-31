package com.eventalert.model;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ChannelRequest(
        @NotNull ChannelType type,
        Map<String, Object> config
) {
}
