package com.eventalert.service;

import com.eventalert.model.ChannelType;

import java.util.Map;

public interface ChannelConfigValidator {

    ChannelType supports();

    /**
     * @throws com.eventalert.exception.InvalidChannelConfigException if config is malformed for this channel type
     */
    void validate(Map<String, Object> config);
}
