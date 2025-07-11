package com.waheed.oasregexauditor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private String location; // Path within OAS (e.g., #/components/schemas/User/properties/email/pattern)
    private String regexPattern;
    private String engine; // Java, JavaScript, Go (RE2J)
    private boolean isValid; // True if valid for the engine
    private String issueType; // ERROR, WARNING, INFO
    private String message; // Detailed message about the validation outcome
    private String suggestion; // Suggestion for improvement or alternative
    private String colorClass; // CSS class for color coding (e.g., 'text-green-600', 'text-red-600')

    /**
     * Helper to create a successful validation result.
     */
    public static ValidationResult success(String location, String regexPattern, String engine) {
        return new ValidationResult(location, regexPattern, engine, true, "INFO", "Valid regex for " + engine + " engine.", "", "text-green-600");
    }

    /**
     * Helper to create an error validation result.
     */
    public static ValidationResult error(String location, String regexPattern, String engine, String message, String suggestion) {
        return new ValidationResult(location, regexPattern, engine, false, "ERROR", message, suggestion, "text-red-600");
    }

    /**
     * Helper to create a warning validation result.
     */
    public static ValidationResult warning(String location, String regexPattern, String engine, String message, String suggestion) {
        return new ValidationResult(location, regexPattern, engine, false, "WARNING", message, suggestion, "text-yellow-600");
    }
}