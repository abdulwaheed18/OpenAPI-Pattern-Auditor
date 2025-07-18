package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.stereotype.Component;

@Component
public class JavaScriptRegexValidator implements RegexValidator {

    private static final String ENGINE_NAME = "JavaScript";

    @Override
    public ValidationResult validate(String location, int lineNumber, String regex) {
        try (Context context = Context.create("js")) {
            String escapedRegex = regex.replace("\\", "\\\\").replace("'", "\\'");
            context.eval("js", "new RegExp('" + escapedRegex + "');");
            return ValidationResult.success(location, lineNumber, regex, ENGINE_NAME);
        } catch (PolyglotException e) {
            String errorMessage = "Invalid JavaScript regex: " + e.getMessage();
            String suggestion = "The pattern is not a valid regular expression according to JavaScript's syntax rules.";
            String suggestedRegex = generateSuggestedFix(regex, e.getMessage());
            return ValidationResult.error(location, lineNumber, regex, ENGINE_NAME, errorMessage, suggestion, suggestedRegex);
        } catch (Exception e) {
            String errorMessage = "An unexpected error occurred during JavaScript validation: " + e.getMessage();
            return ValidationResult.error(location, lineNumber, regex, ENGINE_NAME, errorMessage, "There might be an issue with the GraalVM environment setup.", null);
        }
    }

    private String generateSuggestedFix(String regex, String errorMessage) {
        if (errorMessage.contains("Unterminated character class")) {
            return regex + "]";
        }
        if (errorMessage.contains("Unmatched ')'")) {
            long openParens = regex.chars().filter(ch -> ch == '(').count();
            long closeParens = regex.chars().filter(ch -> ch == ')').count();
            if (openParens > closeParens) {
                return regex + ")";
            }
        }
        return null;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }
}
