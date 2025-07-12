package com.waheed.oasregexauditor.model;

import lombok.Data;
import java.util.List;

@Data
public class GroupedValidationResult {
    private final String location;
    private final int lineNumber;
    private final String regexPattern;
    private final List<ValidationResult> results;

    public GroupedValidationResult(String location, int lineNumber, String regexPattern, List<ValidationResult> results) {
        this.location = location;
        this.lineNumber = lineNumber;
        this.regexPattern = regexPattern;
        this.results = results;
    }

    public boolean hasError() {
        return results.stream().anyMatch(r -> r.getIssueType() == ValidationResult.IssueType.ERROR);
    }

    public boolean hasWarning() {
        return results.stream().anyMatch(r -> r.getIssueType() == ValidationResult.IssueType.WARNING);
    }

    public boolean isValid() {
        return !hasError() && !hasWarning();
    }
}
