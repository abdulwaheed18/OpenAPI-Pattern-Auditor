// File: src/main/java/com/waheed/oasregexauditor/model/ValidationResult.java
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
    private String regexPattern;
    private String engine;
    private boolean isValid;
    private IssueType issueType;
    private String message;
    private String suggestion; // General advice
    private String suggestedRegex; // A concrete, suggested pattern fix

    public static ValidationResult success(String location, String regexPattern, String engine) {
        return new ValidationResult(location, regexPattern, engine, true, IssueType.VALID, "Pattern is valid for the " + engine + " engine.", null, null);
    }

    public static ValidationResult error(String location, String regexPattern, String engine, String message, String suggestion, String suggestedRegex) {
        return new ValidationResult(location, regexPattern, engine, false, IssueType.ERROR, message, suggestion, suggestedRegex);
    }

    public static ValidationResult warning(String location, String regexPattern, String engine, String message, String suggestion, String suggestedRegex) {
        return new ValidationResult(location, regexPattern, engine, true, IssueType.WARNING, message, suggestion, suggestedRegex);
    }
}