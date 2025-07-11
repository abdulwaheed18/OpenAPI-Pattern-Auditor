package com.waheed.oasregexauditor.service.validators;

import com.waheed.oasregexauditor.model.ValidationResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.stereotype.Component;

/**
 * Validator for JavaScript's regex engine using GraalVM.
 * This provides an accurate validation by executing the regex compilation
 * in a real JavaScript environment.
 */
@Component
public class JavaScriptRegexValidator implements RegexValidator {

    private static final String ENGINE_NAME = "JavaScript";

    @Override
    public ValidationResult validate(String location, String regex) {
        // Use a try-with-resources block to ensure the GraalVM context is closed.
        try (Context context = Context.create("js")) {
            // To prevent injection or syntax errors, escape single quotes and backslashes in the regex string
            String escapedRegex = regex.replace("\\", "\\\\").replace("'", "\\'");
            // Create a new RegExp object in JavaScript to validate the pattern
            context.eval("js", "new RegExp('" + escapedRegex + "');");
            return ValidationResult.success(location, regex, ENGINE_NAME);
        } catch (PolyglotException e) {
            // GraalVM throws a PolyglotException for syntax errors in the guest language.
            String errorMessage = "Invalid JavaScript regex: " + e.getMessage();
            String suggestion = "The pattern is not a valid regular expression according to JavaScript's syntax rules. Check for issues like unescaped characters or unsupported constructs.";
            return ValidationResult.error(location, regex, ENGINE_NAME, errorMessage, suggestion);
        } catch (Exception e) {
            // Catch any other unexpected errors during validation.
            String errorMessage = "An unexpected error occurred during JavaScript validation: " + e.getMessage();
            return ValidationResult.error(location, regex, ENGINE_NAME, errorMessage, "There might be an issue with the GraalVM environment setup.");
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }
}
