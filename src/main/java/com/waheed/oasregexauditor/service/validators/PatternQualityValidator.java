package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PatternQualityValidator {

    private static final String ENGINE_NAME = "Quality Check";
    private static final Map<String, String> WEAK_PATTERNS = Map.of(
            ".*", "The pattern '.*' allows any sequence of characters, including an empty string. This is often too permissive.",
            ".+", "The pattern '.+' allows any sequence of one or more characters. It is still very broad."
    );

    public List<ValidationResult> validateRegex(String location, int lineNumber, String regex, boolean checkPermissive, boolean checkAnchors, boolean checkRedos) {
        List<ValidationResult> results = new ArrayList<>();
        String trimmedRegex = regex.trim();

        if (checkPermissive && WEAK_PATTERNS.containsKey(trimmedRegex)) {
            results.add(ValidationResult.warning(location, lineNumber, regex, ENGINE_NAME, "Overly Permissive Pattern", WEAK_PATTERNS.get(trimmedRegex), null));
        }

        if (checkAnchors && !WEAK_PATTERNS.containsKey(trimmedRegex) && (!trimmedRegex.startsWith("^") || !trimmedRegex.endsWith("$"))) {
            String suggestion = "The pattern can match a substring. Consider anchoring it to match the entire string.";
            String suggestedRegex = "^" + trimmedRegex + "$";
            results.add(ValidationResult.warning(location, lineNumber, regex, ENGINE_NAME, "Missing Anchors", suggestion, suggestedRegex));
        }

        if (checkRedos) {
            // This regex is a simplified check for patterns like (a+)+ or (a*)*
            java.util.regex.Pattern redosPattern = java.util.regex.Pattern.compile(".*\\([^?].*[*+]\\)[*+].*");
            if (redosPattern.matcher(regex).matches()) {
                results.add(ValidationResult.warning(location, lineNumber, regex, ENGINE_NAME, "Potential ReDoS Vulnerability", "The pattern appears to contain nested quantifiers (e.g., (a+)+), which can lead to catastrophic backtracking.", null));
            }
        }
        return results;
    }

    public List<ValidationResult> validateOperation(String location, Operation operation, boolean checkOperationId, boolean checkSummary) {
        List<ValidationResult> results = new ArrayList<>();
        // For best-practice checks, line number is not applicable, so we pass 0.
        if (checkOperationId && (operation.getOperationId() == null || operation.getOperationId().isBlank())) {
            results.add(ValidationResult.warning(location, 0, "N/A", ENGINE_NAME, "Missing OperationID", "Each operation should have a unique 'operationId' for code generation and tooling.", null));
        }
        if (checkSummary && (operation.getSummary() == null || operation.getSummary().isBlank())) {
            results.add(ValidationResult.warning(location, 0, "N/A", ENGINE_NAME, "Missing Summary", "A summary provides a quick, human-readable overview of the operation's purpose.", null));
        }
        return results;
    }

    public List<ValidationResult> validateSchema(String location, Schema<?> schema, boolean checkSchemaDescription, boolean checkSchemaExample) {
        List<ValidationResult> results = new ArrayList<>();
        String schemaName = location.substring(location.lastIndexOf('/') + 1);
        if (checkSchemaDescription && (schema.getDescription() == null || schema.getDescription().isBlank())) {
            results.add(ValidationResult.warning(location, 0, schemaName, ENGINE_NAME, "Missing Schema Description", "A description clarifies the purpose and structure of the schema.", null));
        }
        if (checkSchemaExample && schema.getExample() == null) {
            results.add(ValidationResult.warning(location, 0, schemaName, ENGINE_NAME, "Missing Schema Example", "Providing an example value helps developers understand the expected data format.", null));
        }
        return results;
    }

    public List<ValidationResult> validatePath(String location, String path, boolean checkNaming) {
        List<ValidationResult> results = new ArrayList<>();
        if (checkNaming && path.matches(".*[A-Z].*")) {
            results.add(ValidationResult.warning(location, 0, path, ENGINE_NAME, "Path Naming Convention", "Path segments should ideally use kebab-case (e.g., /user-profiles) or snake_case, not camelCase.", null));
        }
        return results;
    }
}
