package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;

/**
 * Interface for a specific regex engine validator.
 * Each implementation will be responsible for validating a regex pattern
 * against a single engine (e.g., Java, JavaScript, Go RE2J).
 */
public interface RegexValidator {

    /**
     * Validates the given regex pattern.
     *
     * @param location The JSON Pointer path to the pattern within the OpenAPI specification.
     * @param regex The regex pattern string to validate.
     * @return A {@link ValidationResult} object containing the outcome of the validation.
     */
    ValidationResult validate(String location, String regex);

    /**
     * Returns the name of the validation engine.
     *
     * @return A string representing the engine name (e.g., "Java").
     */
    String getEngineName();
}
