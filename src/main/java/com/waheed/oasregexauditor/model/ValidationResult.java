package com.waheed.oasregexauditor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    public enum IssueType {
        VALID,
        WARNING,
        ERROR
    }

    private String location;
    private int lineNumber;
    private String regexPattern;
    private String engine;
    private boolean isValid;
    private IssueType issueType;
    private String message;
    private String suggestion;
    private String suggestedRegex;

    public static ValidationResult success(String location, int lineNumber, String regexPattern, String engine) {
        return new ValidationResult(location, lineNumber, regexPattern, engine, true, IssueType.VALID, "Pattern is valid for the " + engine + " engine.", null, null);
    }

    public static ValidationResult error(String location, int lineNumber, String regexPattern, String engine, String message, String suggestion, String suggestedRegex) {
        return new ValidationResult(location, lineNumber, regexPattern, engine, false, IssueType.ERROR, message, suggestion, suggestedRegex);
    }

    public static ValidationResult warning(String location, int lineNumber, String regexPattern, String engine, String message, String suggestion, String suggestedRegex) {
        // **FIXED**: Correctly pass the 'engine' parameter to the constructor.
        return new ValidationResult(location, lineNumber, regexPattern, engine, true, IssueType.WARNING, message, suggestion, suggestedRegex);
    }
}
