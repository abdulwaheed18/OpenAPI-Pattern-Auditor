package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.ValidationResult;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.stereotype.Service;
import com.google.re2j.PatternSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OasValidationService {

    /**
     * Validates regex patterns found in an OpenAPI object against specified engines.
     * @param openAPI The parsed OpenAPI object.
     * @param validateJava True to validate against Java regex.
     * @param validateJs True to validate against JavaScript regex.
     * @param validateGoRe2j True to validate against Go (RE2J) regex.
     * @return A list of ValidationResult objects.
     */
    public List<ValidationResult> validateOasRegex(OpenAPI openAPI, boolean validateJava, boolean validateJs, boolean validateGoRe2j) {
        List<ValidationResult> results = new ArrayList<>();

        // 1. Scan Schemas for 'pattern' properties
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
            for (Map.Entry<String, Schema> schemaEntry : schemas.entrySet()) {
                String schemaName = schemaEntry.getKey();
                Schema schema = schemaEntry.getValue();
                scanSchemaForPatterns(schema, "#/components/schemas/" + schemaName, results, validateJava, validateJs, validateGoRe2j);
            }
        }

        // 2. Scan Path Parameters and Operations for 'pattern' properties
        if (openAPI.getPaths() != null) {
            Map<String, PathItem> paths = openAPI.getPaths();
            for (Map.Entry<String, PathItem> pathEntry : paths.entrySet()) {
                String path = pathEntry.getKey();
                PathItem pathItem = pathEntry.getValue();

                // Check path parameters
                if (pathItem.getParameters() != null) {
                    for (Parameter parameter : pathItem.getParameters()) {
                        if (parameter.getSchema() != null && parameter.getSchema().getPattern() != null) {
                            String regex = parameter.getSchema().getPattern();
                            String location = "#/paths/" + path + "/parameters/" + parameter.getName() + "/schema/pattern";
                            validatePattern(location, regex, results, validateJava, validateJs, validateGoRe2j);
                        }
                    }
                }

                // Check operation parameters within each HTTP method
                pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                    if (operation.getParameters() != null) {
                        for (Parameter parameter : operation.getParameters()) {
                            if (parameter.getSchema() != null && parameter.getSchema().getPattern() != null) {
                                String regex = parameter.getSchema().getPattern();
                                String location = "#/paths/" + path + "/" + httpMethod.name().toLowerCase() + "/parameters/" + parameter.getName() + "/schema/pattern";
                                validatePattern(location, regex, results, validateJava, validateJs, validateGoRe2j);
                            }
                        }
                    }
                    // Check request body schemas for patterns
                    if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                        operation.getRequestBody().getContent().forEach((mediaType, mediaTypeObject) -> {
                            if (mediaTypeObject.getSchema() != null) {
                                scanSchemaForPatterns(mediaTypeObject.getSchema(), "#/paths/" + path + "/" + httpMethod.name().toLowerCase() + "/requestBody/content/" + mediaType + "/schema", results, validateJava, validateJs, validateGoRe2j);
                            }
                        });
                    }
                    // Check response body schemas for patterns
                    if (operation.getResponses() != null) {
                        operation.getResponses().forEach((responseCode, apiResponse) -> {
                            if (apiResponse.getContent() != null) {
                                apiResponse.getContent().forEach((mediaType, mediaTypeObject) -> {
                                    if (mediaTypeObject.getSchema() != null) {
                                        scanSchemaForPatterns(mediaTypeObject.getSchema(), "#/paths/" + path + "/" + httpMethod.name().toLowerCase() + "/responses/" + responseCode + "/content/" + mediaType + "/schema", results, validateJava, validateJs, validateGoRe2j);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }
        return results;
    }

    /**
     * Recursively scans a schema for 'pattern' properties.
     */
    private void scanSchemaForPatterns(Schema schema, String currentLocation, List<ValidationResult> results,
                                       boolean validateJava, boolean validateJs, boolean validateGoRe2j) {
        if (schema == null) {
            return;
        }

        // Check the current schema's pattern
        if (schema.getPattern() != null) {
            validatePattern(currentLocation + "/pattern", schema.getPattern(), results, validateJava, validateJs, validateGoRe2j);
        }

        // Check properties within the schema
        if (schema.getProperties() != null) {
            Map<String, Schema> properties = schema.getProperties();
            // Handle potential raw map by explicit casting
            for (Map.Entry<String, ?> entry : properties.entrySet()) {
                String propName = entry.getKey();
                Object propValue = entry.getValue();

                // Safe cast to Schema
                if (propValue instanceof Schema) {
                    Schema propSchema = (Schema) propValue;
                    scanSchemaForPatterns(propSchema, currentLocation + "/properties/" + propName, results, validateJava, validateJs, validateGoRe2j);
                }
            }
        }

        // Check items in array schemas
        if (schema.getItems() != null) {
            scanSchemaForPatterns(schema.getItems(), currentLocation + "/items", results, validateJava, validateJs, validateGoRe2j);
        }

        // Check 'allOf', 'anyOf', 'oneOf'
        if (schema.getAllOf() != null) {
            for (int i = 0; i < schema.getAllOf().size(); i++) {
                Object allOfObject = schema.getAllOf().get(i);
                if (allOfObject instanceof Schema) {
                    Schema allOfSchema = (Schema) allOfObject;
                    scanSchemaForPatterns(allOfSchema, currentLocation + "/allOf/" + i, results, validateJava, validateJs, validateGoRe2j);
                }
            }
        }
        if (schema.getAnyOf() != null) {
            for (int i = 0; i < schema.getAnyOf().size(); i++) {
                Object anyOfObject = schema.getAnyOf().get(i);
                if (anyOfObject instanceof Schema) {
                    Schema anyOfSchema = (Schema) anyOfObject;
                    scanSchemaForPatterns(anyOfSchema, currentLocation + "/anyOf/" + i, results, validateJava, validateJs, validateGoRe2j);
                }
            }
        }
        if (schema.getOneOf() != null) {
            for (int i = 0; i < schema.getOneOf().size(); i++) {
                Object oneOfObject = schema.getOneOf().get(i);
                if (oneOfObject instanceof Schema) {
                    Schema oneOfSchema = (Schema) oneOfObject;
                    scanSchemaForPatterns(oneOfSchema, currentLocation + "/oneOf/" + i, results, validateJava, validateJs, validateGoRe2j);
                }
            }
        }

        // Check additional properties if it's a Schema
        if (schema.getAdditionalProperties() != null && schema.getAdditionalProperties() instanceof Schema) {
            Schema additionalPropsSchema = (Schema) schema.getAdditionalProperties();
            scanSchemaForPatterns(additionalPropsSchema, currentLocation + "/additionalProperties", results, validateJava, validateJs, validateGoRe2j);
        }
    }

    /**
     * Validates a single regex pattern against selected engines and adds results to the list.
     */
    private void validatePattern(String location, String regex, List<ValidationResult> results,
                                 boolean validateJava, boolean validateJs, boolean validateGoRe2j) {
        if (validateJava) {
            validateJavaRegex(location, regex, results);
        }
        if (validateJs) {
            validateJavaScriptRegex(location, regex, results);
        }
        if (validateGoRe2j) {
            validateGoRe2jRegex(location, regex, results);
        }
    }

    /**
     * Validates a regex pattern against Java's regex engine.
     */
    private void validateJavaRegex(String location, String regex, List<ValidationResult> results) {
        try {
            Pattern.compile(regex);
            results.add(ValidationResult.success(location, regex, "Java"));
        } catch (java.util.regex.PatternSyntaxException e) {
            results.add(ValidationResult.error(location, regex, "Java",
                    "Invalid Java regex syntax: " + e.getMessage(),
                    "Check Java regex documentation for syntax rules."));
        }
    }

    /**
     * Validates a regex pattern against a simplified JavaScript regex check.
     * Note: Full JavaScript regex behavior validation would require an embedded JS engine (e.g., GraalVM JS).
     * This method primarily checks for common syntax issues that might break JS.
     */
    private void validateJavaScriptRegex(String location, String regex, List<ValidationResult> results) {
        // Simple check: many Java regex patterns are compatible with JS.
        // The main differences are lookbehinds, named capture groups, and some flags.
        // For a full validation, you'd need a JS engine.
        // Here, we'll flag some known incompatibilities as warnings.
        String message = "Potentially compatible with JavaScript regex. Full behavioral validation requires a JS engine.";
        String suggestion = "Consider common regex patterns for broader compatibility. Avoid lookbehinds if not strictly necessary.";
        String issueType = "INFO";
        String colorClass = "text-green-600";

        if (regex.contains("(?<") || regex.contains("(?<!")) { // Named capture groups or lookbehinds
            message = "Possible incompatibility: JavaScript regex prior to ES2018 does not support lookbehinds or named capture groups. ES2018+ supports lookbehinds.";
            suggestion = "Consider refactoring to avoid lookbehinds or named capture groups for broader JS compatibility, or ensure target JS environments support ES2018+.";
            issueType = "WARNING";
            colorClass = "text-yellow-600";
        } else if (regex.contains("\\A") || regex.contains("\\Z")) { // Start/end of string (Java/Perl specific)
            message = "Possible incompatibility: \\A and \\Z are not standard in JavaScript regex. Use ^ and $ instead.";
            suggestion = "Replace \\A with ^ and \\Z with $ for JavaScript compatibility.";
            issueType = "WARNING";
            colorClass = "text-yellow-600";
        }

        try {
            // Attempt to compile with Java, as many JS regex are Java-compatible.
            // This is a weak check for JS, but better than nothing without a JS engine.
            Pattern.compile(regex);
            results.add(new ValidationResult(location, regex, "JavaScript", true, issueType, message, suggestion, colorClass));
        } catch (java.util.regex.PatternSyntaxException e) {
            results.add(ValidationResult.error(location, regex, "JavaScript",
                    "Invalid JavaScript regex syntax (based on Java compatibility check): " + e.getMessage(),
                    "Check JavaScript regex documentation for syntax rules."));
        }
    }

    /**
     * Validates a regex pattern against Google's RE2J (Go-style) regex engine.
     */
    private void validateGoRe2jRegex(String location, String regex, List<ValidationResult> results) {
        try {
            com.google.re2j.Pattern.compile(regex);
            results.add(ValidationResult.success(location, regex, "Go (RE2J)"));
        } catch (PatternSyntaxException e) {
            results.add(ValidationResult.error(location, regex, "Go (RE2J)",
                    "Invalid Go (RE2J) regex syntax or unsupported feature: " + e.getMessage(),
                    "RE2J is a simpler, faster regex engine. Avoid lookarounds, backreferences, and complex assertions. Use atomic groups (?>...) for non-capturing groups if needed."));
        }
    }
}