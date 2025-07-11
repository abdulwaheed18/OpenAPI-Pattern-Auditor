package com.waheed.oasregexauditor.model;

import lombok.Data;
import java.util.List;

/**
 * Represents a collection of validation results grouped by a common location and pattern.
 */
@Data
public class GroupedValidationResult {
    private final String location;
    private final String regexPattern;
    private final List<ValidationResult> results;

    public GroupedValidationResult(String location, String regexPattern, List<ValidationResult> results) {
        this.location = location;
        this.regexPattern = regexPattern;
        this.results = results;
    }

    // Helper method to determine the overall status of the group for filtering
    public boolean hasError() {
        return results.stream().anyMatch(r -> r.getIssueType() == ValidationResult.IssueType.ERROR);
    }

    public boolean hasWarning() {
        return results.stream().anyMatch(r -> r.getIssueType() == ValidationResult.IssueType.WARNING);
    }

    public boolean isValid() {
        // A group is considered "Valid" if it has no errors or warnings.
        return !hasError() && !hasWarning();
    }
}
