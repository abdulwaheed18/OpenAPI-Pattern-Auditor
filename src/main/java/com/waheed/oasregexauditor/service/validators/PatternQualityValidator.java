package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * A specialized validator that checks for common quality and security issues in regex patterns.
 * This runs in addition to the engine-specific syntax validators.
 */
@Component
public class PatternQualityValidator {

    private static final String ENGINE_NAME = "Quality Check";

    // A map of common weak patterns and their corresponding suggestions.
    private static final Map<String, String> WEAK_PATTERNS = Map.of(
            ".*", "The pattern '.*' allows any sequence of characters, including an empty string. This is often too permissive and can lead to validation bypasses or unexpected behavior.",
            ".+", "The pattern '.+' allows any sequence of one or more characters. While slightly more restrictive than '.*', it is still very broad. Specify the character set and length if possible."
    );

    /**
     * Runs a series of quality checks on the given regex pattern.
     *
     * @param location The JSON Pointer path to the pattern.
     * @param regex    The regex pattern string to analyze.
     * @return A list of ValidationResult objects with WARNING severity for any issues found.
     */
    public List<ValidationResult> validate(String location, String regex) {
        List<ValidationResult> results = new ArrayList<>();
        String trimmedRegex = regex.trim();

        // 1. Check for overly permissive patterns like '.*'
        if (WEAK_PATTERNS.containsKey(trimmedRegex)) {
            results.add(ValidationResult.warning(
                    location,
                    regex,
                    ENGINE_NAME,
                    "Overly Permissive Pattern",
                    WEAK_PATTERNS.get(trimmedRegex)
            ));
        }

        // 2. Check for missing start/end anchors, which is a very common mistake.
        // We avoid flagging already identified weak patterns which are permissive by nature.
        if (!WEAK_PATTERNS.containsKey(trimmedRegex) && (!trimmedRegex.startsWith("^") || !trimmedRegex.endsWith("$"))) {
            results.add(ValidationResult.warning(
                    location,
                    regex,
                    ENGINE_NAME,
                    "Missing Anchors",
                    "The pattern is not anchored with '^' at the start and '$' at the end. This means it can match a substring within a larger, invalid string. Consider wrapping the pattern with '^' and '$' to ensure it matches the entire string."
            ));
        }

        // 3. Check for signs of potential ReDoS (Regular Expression Denial of Service) vulnerabilities.
        // This is a basic check for "evil regex" patterns like nested quantifiers, e.g., (a+)+
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

        return results;
    }
}
