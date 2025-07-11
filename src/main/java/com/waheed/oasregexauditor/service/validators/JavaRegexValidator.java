package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Validator for Java's built-in regex engine (java.util.regex).
 */
@Component
public class JavaRegexValidator implements RegexValidator {

    private static final String ENGINE_NAME = "Java";

    @Override
    public ValidationResult validate(String location, String regex) {
        try {
            Pattern.compile(regex);
            return ValidationResult.success(location, regex, ENGINE_NAME);
        } catch (PatternSyntaxException e) {
            // Provide a more helpful message by pinpointing the error
            String errorMessage = String.format("Invalid Java regex syntax: %s near index %d", e.getDescription(), e.getIndex());
            String suggestion = String.format("Review the pattern syntax around: '%s'. Check Java's regex documentation for supported features.", e.getPattern());
            return ValidationResult.error(location, regex, ENGINE_NAME, errorMessage, suggestion);
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }
}
