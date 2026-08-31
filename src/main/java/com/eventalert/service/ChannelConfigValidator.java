package com.eventalert.service;

import com.eventalert.model.ChannelType;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * Validates a channel's config for its type — one implementation per {@link ChannelType}.
 */
public interface ChannelConfigValidator {

    @NonNull
    ChannelType supports();

    /**
     * @throws com.eventalert.exception.InvalidChannelConfigException if config is malformed for this channel type
     */
    void validate(@NonNull Map<String, Object> config);
}
