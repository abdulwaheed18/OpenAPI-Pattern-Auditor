package com.waheed.oasregexauditor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of a single regex pattern validation against a specific engine.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private String location;     // JSON Pointer path within the OAS document (e.g., #/components/schemas/User/properties/email/pattern)
    private String regexPattern; // The actual regex pattern string
    private String engine;       // The validation engine used (e.g., "Java", "JavaScript", "Go (RE2J)")
    private boolean isValid;     // True if the pattern is valid for the engine, false otherwise
    private String issueType;    // Severity level: "VALID", "ERROR", "WARNING"
    private String message;      // A detailed message explaining the validation outcome
    private String suggestion;   // An optional suggestion for fixing or improving the pattern

    /**
     * Helper method to create a successful validation result.
     *
     * @param location     The JSON Pointer location of the pattern.
     * @param regexPattern The validated regex pattern.
     * @param engine       The engine that performed the validation.
     * @return A new ValidationResult instance representing success.
     */
    public static ValidationResult success(String location, String regexPattern, String engine) {
        return new ValidationResult(location, regexPattern, engine, true, "VALID", "Pattern is valid for the " + engine + " engine.", "");
    }

    /**
     * Helper method to create an error validation result.
     *
     * @param location     The JSON Pointer location of the pattern.
     * @param regexPattern The validated regex pattern.
     * @param engine       The engine that performed the validation.
     * @param message      The error message.
     * @param suggestion   A suggestion for fixing the error.
     * @return A new ValidationResult instance representing an error.
     */
    public static ValidationResult error(String location, String regexPattern, String engine, String message, String suggestion) {
        return new ValidationResult(location, regexPattern, engine, false, "ERROR", message, suggestion);
    }

    /**
     * Helper method to create a warning validation result.
     *
     * @param location     The JSON Pointer location of the pattern.
     * @param regexPattern The validated regex pattern.
     * @param engine       The engine that performed the validation.
     * @param message      The warning message.
     * @param suggestion   A suggestion for improvement.
     * @return A new ValidationResult instance representing a warning.
     */
    public static ValidationResult warning(String location, String regexPattern, String engine, String message, String suggestion) {
        return new ValidationResult(location, regexPattern, engine, false, "WARNING", message, suggestion);
    }
}
