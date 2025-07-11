package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.ValidationResult;
import com.waheed.oasregexauditor.service.validators.RegexValidator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main service to orchestrate the validation of regex patterns within an OpenAPI specification.
 * It traverses the OpenAPI document model and uses specific validator components for each selected engine.
 */
@Service
public class OasValidationService {

    private static final Logger log = LoggerFactory.getLogger(OasValidationService.class);

    private final List<RegexValidator> validators;

    @Autowired
    public OasValidationService(List<RegexValidator> validators) {
        this.validators = validators;
    }

    /**
     * Validates all regex patterns found in an OpenAPI object against the selected engines.
     *
     * @param openAPI The parsed OpenAPI object.
     * @param validateJava True to validate against the Java regex engine.
     * @param validateJs True to validate against the JavaScript regex engine.
     * @param validateGoRe2j True to validate against the Go (RE2J) regex engine.
     * @return A list of all validation results.
     */
    public List<ValidationResult> validateOasRegex(OpenAPI openAPI, boolean validateJava, boolean validateJs, boolean validateGoRe2j) {
        List<ValidationResult> results = new ArrayList<>();
        List<RegexValidator> activeValidators = getActiveValidators(validateJava, validateJs, validateGoRe2j);

        if (activeValidators.isEmpty()) {
            log.warn("No validation engines selected. Skipping validation.");
            return results; // No validators selected, return empty list.
        }

        // Consumer to process a found pattern with all active validators
        Consumer<PatternLocation> patternProcessor = (loc) -> {
            for (RegexValidator validator : activeValidators) {
                results.add(validator.validate(loc.path, loc.pattern));
            }
        };

        // 1. Scan component schemas
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            openAPI.getComponents().getSchemas().forEach((schemaName, schema) ->
                    scanSchemaForPatterns(schema, "#/components/schemas/" + schemaName, patternProcessor)
            );
        }

        // 2. Scan paths for parameters, request bodies, and responses
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) ->
                    scanPathItem(path, pathItem, patternProcessor)
            );
        }

        return results;
    }

    /**
     * Filters the available validators based on user selection.
     */
    private List<RegexValidator> getActiveValidators(boolean java, boolean js, boolean go) {
        List<RegexValidator> active = new ArrayList<>();
        for (RegexValidator validator : validators) {
            if (java && "Java".equals(validator.getEngineName())) active.add(validator);
            if (js && "JavaScript".equals(validator.getEngineName())) active.add(validator);
            if (go && "Go (RE2J)".equals(validator.getEngineName())) active.add(validator);
        }
        return active;
    }

    /**
     * Scans a PathItem for regex patterns in its parameters and operations.
     */
    private void scanPathItem(String path, PathItem pathItem, Consumer<PatternLocation> processor) {
        String pathPrefix = "#/paths/" + escapePath(path);

        // Path-level parameters
        if (pathItem.getParameters() != null) {
            for (int i = 0; i < pathItem.getParameters().size(); i++) {
                Parameter param = pathItem.getParameters().get(i);
                if (param.getSchema() != null && param.getSchema().getPattern() != null) {
                    processor.accept(new PatternLocation(
                            pathPrefix + "/parameters/" + i + "/schema/pattern",
                            param.getSchema().getPattern()
                    ));
                }
            }
        }

        // Operation-level scanning (GET, POST, etc.)
        pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
            String opPrefix = pathPrefix + "/" + httpMethod.name().toLowerCase();

            // Operation parameters
            if (operation.getParameters() != null) {
                for (int i = 0; i < operation.getParameters().size(); i++) {
                    Parameter param = operation.getParameters().get(i);
                    if (param.getSchema() != null && param.getSchema().getPattern() != null) {
                        processor.accept(new PatternLocation(
                                opPrefix + "/parameters/" + i + "/schema/pattern",
                                param.getSchema().getPattern()
                        ));
                    }
                }
            }

            // Request Body
            if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                operation.getRequestBody().getContent().forEach((mediaType, mediaTypeObject) -> {
                    if (mediaTypeObject.getSchema() != null) {
                        scanSchemaForPatterns(mediaTypeObject.getSchema(), opPrefix + "/requestBody/content/" + escapePath(mediaType) + "/schema", processor);
                    }
                });
            }

            // Responses
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((responseCode, apiResponse) -> {
                    if (apiResponse.getContent() != null) {
                        apiResponse.getContent().forEach((mediaType, mediaTypeObject) -> {
                            if (mediaTypeObject.getSchema() != null) {
                                scanSchemaForPatterns(mediaTypeObject.getSchema(), opPrefix + "/responses/" + responseCode + "/content/" + escapePath(mediaType) + "/schema", processor);
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Recursively scans a Schema object and its children for 'pattern' properties.
     */
    private void scanSchemaForPatterns(Schema<?> schema, String currentLocation, Consumer<PatternLocation> processor) {
        if (schema == null) {
            return;
        }

        // Check the pattern on the current schema
        if (schema.getPattern() != null) {
            processor.accept(new PatternLocation(currentLocation + "/pattern", schema.getPattern()));
        }

        // Recursively scan properties
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((propName, propSchema) ->
                    scanSchemaForPatterns((Schema<?>) propSchema, currentLocation + "/properties/" + propName, processor)
            );
        }

        // Recursively scan array items
        if (schema.getItems() != null) {
            scanSchemaForPatterns(schema.getItems(), currentLocation + "/items", processor);
        }

        // Recursively scan composition keywords
        scanComposition(schema.getAllOf(), "allOf", currentLocation, processor);
        scanComposition(schema.getAnyOf(), "anyOf", currentLocation, processor);
        scanComposition(schema.getOneOf(), "oneOf", currentLocation, processor);

        // Recursively scan additionalProperties if it's a schema
        if (schema.getAdditionalProperties() instanceof Schema) {
            scanSchemaForPatterns((Schema<?>) schema.getAdditionalProperties(), currentLocation + "/additionalProperties", processor);
        }
    }

    /**
     * Helper to scan composition arrays (allOf, anyOf, oneOf).
     */
    private void scanComposition(List<Schema> schemas, String keyword, String currentLocation, Consumer<PatternLocation> processor) {
        if (schemas != null) {
            for (int i = 0; i < schemas.size(); i++) {
                scanSchemaForPatterns(schemas.get(i), currentLocation + "/" + keyword + "/" + i, processor);
            }
        }
    }

    /**
     * Escapes characters in a path segment for use in a JSON Pointer.
     * ~ becomes ~0 and / becomes ~1.
     */
    private String escapePath(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }

    /**
     * Helper record to hold a pattern and its location.
     */
    private record PatternLocation(String path, String pattern) {}
}
