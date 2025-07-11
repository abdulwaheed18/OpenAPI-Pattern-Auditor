package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * A specialized validator that checks for common quality and security issues in regex patterns
 * and other API design best practices.
 */
@Component
public class PatternQualityValidator {

    private static final String ENGINE_NAME = "Quality Check";

    private static final Map<String, String> WEAK_PATTERNS = Map.of(
            ".*", "The pattern '.*' allows any sequence of characters, including an empty string. This is often too permissive and can lead to validation bypasses or unexpected behavior.",
            ".+", "The pattern '.+' allows any sequence of one or more characters. While slightly more restrictive than '.*', it is still very broad. Specify the character set and length if possible."
    );

    /**
     * Runs a series of quality checks on a regex pattern.
     */
    public List<ValidationResult> validateRegex(String location, String regex, boolean checkPermissive, boolean checkAnchors, boolean checkRedos) {
        List<ValidationResult> results = new ArrayList<>();
        String trimmedRegex = regex.trim();

        // 1. Check for overly permissive patterns
        if (checkPermissive && WEAK_PATTERNS.containsKey(trimmedRegex)) {
            results.add(ValidationResult.warning(
                    location,
                    regex,
                    ENGINE_NAME,
                    "Overly Permissive Pattern",
                    WEAK_PATTERNS.get(trimmedRegex)
            ));
        }

        // 2. Check for missing start/end anchors
        if (checkAnchors && !WEAK_PATTERNS.containsKey(trimmedRegex) && (!trimmedRegex.startsWith("^") || !trimmedRegex.endsWith("$"))) {
            results.add(ValidationResult.warning(
                    location,
                    regex,
                    ENGINE_NAME,
                    "Missing Anchors",
                    "The pattern is not anchored with '^' and '$' at the start and end. This means it can match a substring within a larger, invalid string. Consider wrapping the pattern with '^' and '$' to ensure it matches the entire string."
            ));
        }

        // 3. Check for signs of potential ReDoS
        if (checkRedos) {
            java.util.regex.Pattern redosPattern = java.util.regex.Pattern.compile(".*\\([^?].*[*+]\\)[*+].*");
            Matcher matcher = redosPattern.matcher(regex);
            if (matcher.matches()) {
                results.add(ValidationResult.warning(
                        location,
                        regex,
                        ENGINE_NAME,
                        "Potential ReDoS Vulnerability",
                        "The pattern appears to contain nested quantifiers (e.g., (a+)+), which can lead to catastrophic backtracking on certain inputs. This is a security risk. Consider refactoring the expression to be more efficient and avoid this structure."
                ));
            }
        }
        return results;
    }

    /**
     * NEW: Validates an API Operation for best practices.
     */
    public List<ValidationResult> validateOperation(String location, Operation operation, boolean checkOperationId, boolean checkSummary) {
        List<ValidationResult> results = new ArrayList<>();

        if (checkOperationId && (operation.getOperationId() == null || operation.getOperationId().isBlank())) {
            results.add(ValidationResult.warning(location, "N/A", ENGINE_NAME, "Missing OperationID", "Each operation should have a unique 'operationId' for code generation and tooling."));
        }

        if (checkSummary && (operation.getSummary() == null || operation.getSummary().isBlank())) {
            results.add(ValidationResult.warning(location, "N/A", ENGINE_NAME, "Missing Summary", "A summary provides a quick, human-readable overview of the operation's purpose."));
        }
        return results;
    }

    /**
     * NEW: Validates a schema for completeness.
     */
    public List<ValidationResult> validateSchema(String location, Schema<?> schema, boolean checkSchemaDescription, boolean checkSchemaExample) {
        List<ValidationResult> results = new ArrayList<>();
        String schemaName = location.substring(location.lastIndexOf('/') + 1);

        if (checkSchemaDescription && (schema.getDescription() == null || schema.getDescription().isBlank())) {
            results.add(ValidationResult.warning(location, schemaName, ENGINE_NAME, "Missing Schema Description", "A description clarifies the purpose and structure of the schema."));
        }

        if (checkSchemaExample && schema.getExample() == null) {
            results.add(ValidationResult.warning(location, schemaName, ENGINE_NAME, "Missing Schema Example", "Providing an example value helps developers understand the expected data format."));
        }
        return results;
    }

    /**
     * NEW: Validates path for naming conventions.
     */
    public List<ValidationResult> validatePath(String location, String path, boolean checkNaming) {
        List<ValidationResult> results = new ArrayList<>();
        // Simple check for kebab-case or snake_case in path segments
        if (checkNaming && path.matches(".*[A-Z].*")) {
            results.add(ValidationResult.warning(
                    location,
                    path,
                    ENGINE_NAME,
                    "Path Naming Convention",
                    "Path segments should ideally use kebab-case (e.g., /user-profiles) or snake_case, not camelCase, for better readability."
            ));
        }
        return results;
    }
}