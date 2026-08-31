package com.eventalert.service;

import com.eventalert.model.Category;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dispatcher component that routes alert rule criteria validation to the appropriate strategy.
 * <p>
 * Automatically collects all available {@link CriteriaValidator} beans and maps them by their
 * supported event {@link Category} for dynamic delegation during rule criteria validation.
 */
@Component
public class CriteriaValidatorDispatcher {

    private final Map<Category, CriteriaValidator> validatorsByCategory;

    public CriteriaValidatorDispatcher(@NonNull List<CriteriaValidator> validators) {
        this.validatorsByCategory = validators.stream()
                .collect(Collectors.toMap(CriteriaValidator::supports, v -> v));
    }

    public void validate(Category category, Map<String, Object> criteria) {
        CriteriaValidator validator = validatorsByCategory.get(category);
        if (validator == null) {
            // Should only happen if a new Category is added without a matching validator.
            throw new IllegalStateException("No criteria validator registered for category " + category);
        }
        validator.validate(criteria);
    }
}
