package com.waheed.oasregexauditor.service.validators;

import com.google.re2j.PatternSyntaxException;
import com.waheed.oasregexauditor.model.ValidationResult;
import org.springframework.stereotype.Component;

@Component
public class GoRe2jRegexValidator implements RegexValidator {

    private static final String ENGINE_NAME = "Go (RE2J)";

    @Override
    public ValidationResult validate(String location, int lineNumber, String regex) {
        try {
            com.google.re2j.Pattern.compile(regex);
            return ValidationResult.success(location, lineNumber, regex, ENGINE_NAME);
        } catch (PatternSyntaxException e) {
            String errorMessage = String.format("Invalid Go (RE2J) syntax or unsupported feature: %s", e.getMessage());
            String suggestion = "RE2 is designed for efficiency and does not support all PCRE features like lookarounds or backreferences.";
            String suggestedRegex = null;
            if (e.getMessage().contains("missing closing ]")) {
                suggestedRegex = regex + "]";
            }
            return ValidationResult.error(location, lineNumber, regex, ENGINE_NAME, errorMessage, suggestion, suggestedRegex);
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }
}
