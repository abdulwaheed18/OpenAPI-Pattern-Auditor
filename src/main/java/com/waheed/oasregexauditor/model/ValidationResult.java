package com.waheed.oasregexauditor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * Enum to represent the severity of the validation issue.
     */
    public enum IssueType {
        VALID,
        WARNING,
        ERROR
    }

    private String location;
    private String regexPattern;
    private String engine;
    private boolean isValid; // Represents syntax validity. A pattern with a WARNING is still syntactically valid.
    private IssueType issueType;
    private String message;
    private String suggestion;

    public static ValidationResult success(String location, String regexPattern, String engine) {
        return new ValidationResult(location, regexPattern, engine, true, IssueType.VALID, "Pattern is valid for the " + engine + " engine.", null);
    }

    public static ValidationResult error(String location, String regexPattern, String engine, String message, String suggestion) {
        return new ValidationResult(location, regexPattern, engine, false, IssueType.ERROR, message, suggestion);
    }

    public static ValidationResult warning(String location, String regexPattern, String engine, String message, String suggestion) {
        // A warning indicates a quality issue, but the pattern itself is syntactically valid.
        return new ValidationResult(location, regexPattern, engine, true, IssueType.WARNING, message, suggestion);
    }
}
