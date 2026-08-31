package com.eventalert.service;

import com.eventalert.model.ChannelType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dispatcher component that routes channel configuration validation to the appropriate strategy.
 * <p>
 * Automatically collects all available {@link ChannelConfigValidator} beans and maps them by their
 * supported {@link ChannelType} for dynamic delegation during payload validation.
 */
@Component
public class ChannelConfigValidatorDispatcher {

    private final Map<ChannelType, ChannelConfigValidator> validatorsByType;

    public ChannelConfigValidatorDispatcher(@NonNull List<ChannelConfigValidator> validators) {
        this.validatorsByType = validators.stream()
                .collect(Collectors.toMap(ChannelConfigValidator::supports, v -> v));
    }

    public void validate(@NonNull ChannelType type, @NonNull Map<String, Object> config) {
        ChannelConfigValidator validator = validatorsByType.get(type);
        if (validator == null) {
            throw new IllegalStateException("No channel config validator registered for type " + type);
        }
        validator.validate(config);
    }
}
