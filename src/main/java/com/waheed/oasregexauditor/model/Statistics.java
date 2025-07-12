package com.waheed.oasregexauditor.model;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A data class to hold detailed statistics about the validation results.
 */
@Data
public class Statistics {

    private long totalPatterns;
    private long totalErrors;
    private long totalWarnings;
    private long totalValid;

    private Map<String, Long> errorsByEngine;
    private Map<String, Long> warningsByType;

    /**
     * Factory method to create and populate a Statistics object from a list of results.
     * @param results The list of GroupedValidationResult from the analysis.
     * @return A populated Statistics object.
     */
    public static Statistics fromResults(List<GroupedValidationResult> results) {
        Statistics stats = new Statistics();
        stats.totalPatterns = results.size();
        stats.totalValid = results.stream().filter(GroupedValidationResult::isValid).count();

        List<GroupedValidationResult> resultsWithIssues = results.stream()
                .filter(r -> !r.isValid())
                .collect(Collectors.toList());

        stats.totalErrors = results.stream().filter(GroupedValidationResult::hasError).count();
        stats.totalWarnings = results.stream().filter(r -> r.hasWarning() && !r.hasError()).count();

        // Calculate errors by engine
        stats.errorsByEngine = results.stream()
                .flatMap(g -> g.getResults().stream())
                .filter(r -> r.getIssueType() == ValidationResult.IssueType.ERROR)
                .collect(Collectors.groupingBy(ValidationResult::getEngine, Collectors.counting()));

        // Calculate warnings by type (message)
        stats.warningsByType = results.stream()
                .flatMap(g -> g.getResults().stream())
                .filter(r -> r.getIssueType() == ValidationResult.IssueType.WARNING)
                .collect(Collectors.groupingBy(ValidationResult::getMessage, Collectors.counting()));

        return stats;
    }
}
