package com.eventalert.service;

import com.eventalert.model.ChannelType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChannelConfigValidatorDispatcher {

    private final Map<ChannelType, ChannelConfigValidator> validatorsByType;

    public ChannelConfigValidatorDispatcher(List<ChannelConfigValidator> validators) {
        this.validatorsByType = validators.stream()
                .collect(Collectors.toMap(ChannelConfigValidator::supports, v -> v));
    }

    public void validate(ChannelType type, Map<String, Object> config) {
        ChannelConfigValidator validator = validatorsByType.get(type);
        if (validator == null) {
            throw new IllegalStateException("No channel config validator registered for type " + type);
        }
        validator.validate(config);
    }
}
