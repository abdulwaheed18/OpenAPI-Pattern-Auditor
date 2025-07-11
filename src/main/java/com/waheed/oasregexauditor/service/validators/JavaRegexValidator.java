package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component
public class JavaRegexValidator implements RegexValidator {

    private static final String ENGINE_NAME = "Java";

    @Override
    public ValidationResult validate(String location, String regex) {
        try {
            Pattern.compile(regex);
            return ValidationResult.success(location, regex, ENGINE_NAME);
        } catch (PatternSyntaxException e) {
            String errorMessage = String.format("Invalid Java regex syntax: %s near index %d", e.getDescription(), e.getIndex());
            String suggestion = String.format("Review the pattern syntax around: '%s'. Check Java's regex documentation for supported features.", e.getPattern());

            // Generate a suggested fix for common, simple errors
            String suggestedRegex = generateSuggestedFix(regex, e);

            return ValidationResult.error(location, regex, ENGINE_NAME, errorMessage, suggestion, suggestedRegex);
        }
    }

    private String generateSuggestedFix(String regex, PatternSyntaxException e) {
        if (e.getDescription().startsWith("Unclosed character class")) {
            return regex + "]";
        }
        if (e.getDescription().startsWith("Unclosed group")) {
            return regex + ")";
        }
        // Can add more simple fixes here for other common errors
        return null;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }
}