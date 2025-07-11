package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.ValidationResult;
import com.waheed.oasregexauditor.service.validators.PatternQualityValidator;
import com.waheed.oasregexauditor.service.validators.RegexValidator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
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

@Service
public class OasValidationService {

    private static final Logger log = LoggerFactory.getLogger(OasValidationService.class);

    private final List<RegexValidator> validators;
    private final PatternQualityValidator qualityValidator;

    @Autowired
    public OasValidationService(List<RegexValidator> validators, PatternQualityValidator qualityValidator) {
        this.validators = validators;
        this.qualityValidator = qualityValidator;
    }

    public List<ValidationResult> validateOasRegex(OpenAPI openAPI,
                                                   boolean validateJava, boolean validateJs, boolean validateGoRe2j,
                                                   boolean qualityCheckPermissive, boolean qualityCheckAnchors, boolean qualityCheckRedos,
                                                   boolean checkNaming, boolean checkOperationId, boolean checkSummary, boolean checkSchemaDescription, boolean checkSchemaExample) {
        List<ValidationResult> results = new ArrayList<>();
        List<RegexValidator> activeValidators = getActiveValidators(validateJava, validateJs, validateGoRe2j);

        // Consumer to process a found pattern
        Consumer<PatternLocation> patternProcessor = (loc) -> {
            if (!activeValidators.isEmpty()) {
                for (RegexValidator validator : activeValidators) {
                    results.add(validator.validate(loc.path, loc.pattern));
                }
            }
            results.addAll(qualityValidator.validateRegex(loc.path, loc.pattern, qualityCheckPermissive, qualityCheckAnchors, qualityCheckRedos));
        };

        // Scan component schemas
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            openAPI.getComponents().getSchemas().forEach((schemaName, schema) ->
                    {
                        String location = "#/components/schemas/" + schemaName;
                        scanSchemaForPatterns(schema, location, patternProcessor);
                        results.addAll(qualityValidator.validateSchema(location, schema, checkSchemaDescription, checkSchemaExample));
                    }
            );
        }

        // Scan paths
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) ->
                    {
                        String pathLocation = "#/paths/" + escapePath(path);
                        results.addAll(qualityValidator.validatePath(pathLocation, path, checkNaming));
                        scanPathItem(path, pathItem, patternProcessor, results, checkOperationId, checkSummary);
                    }
            );
        }

        return results;
    }

    private List<RegexValidator> getActiveValidators(boolean java, boolean js, boolean go) {
        List<RegexValidator> active = new ArrayList<>();
        for (RegexValidator validator : validators) {
            if (java && "Java".equals(validator.getEngineName())) active.add(validator);
            if (js && "JavaScript".equals(validator.getEngineName())) active.add(validator);
            if (go && "Go (RE2J)".equals(validator.getEngineName())) active.add(validator);
        }
        return active;
    }

    private void scanPathItem(String path, PathItem pathItem, Consumer<PatternLocation> processor, List<ValidationResult> results, boolean checkOperationId, boolean checkSummary) {
        String pathPrefix = "#/paths/" + escapePath(path);

        if (pathItem.getParameters() != null) {
            for (int i = 0; i < pathItem.getParameters().size(); i++) {
                Parameter param = pathItem.getParameters().get(i);
                if (param.getSchema() != null && param.getSchema().getPattern() != null) {
                    processor.accept(new PatternLocation(pathPrefix + "/parameters/" + i, param.getSchema().getPattern()));
                }
            }
        }

        pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
            String opPrefix = pathPrefix + "/" + httpMethod.name().toLowerCase();
            // NEW: Validate operation
            results.addAll(qualityValidator.validateOperation(opPrefix, operation, checkOperationId, checkSummary));

            if (operation.getParameters() != null) {
                for (int i = 0; i < operation.getParameters().size(); i++) {
                    Parameter param = operation.getParameters().get(i);
                    if (param.getSchema() != null && param.getSchema().getPattern() != null) {
                        processor.accept(new PatternLocation(opPrefix + "/parameters/" + i, param.getSchema().getPattern()));
                    }
                }
            }
            if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                operation.getRequestBody().getContent().forEach((mediaType, mediaTypeObject) -> {
                    scanSchemaForPatterns(mediaTypeObject.getSchema(), opPrefix + "/requestBody/content/" + escapePath(mediaType) + "/schema", processor);
                });
            }
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((code, response) -> {
                    if (response.getContent() != null) {
                        response.getContent().forEach((mediaType, mediaTypeObject) -> {
                            scanSchemaForPatterns(mediaTypeObject.getSchema(), opPrefix + "/responses/" + code + "/content/" + escapePath(mediaType) + "/schema", processor);
                        });
                    }
                });
            }
        });
    }

    // Unchanged methods: scanSchemaForPatterns, scanComposition, escapePath, PatternLocation record
    // ...
    /**
     * Recursively scans a Schema object, now with resilient parsing.
     */
    private void scanSchemaForPatterns(Schema<?> schema, String currentLocation, Consumer<PatternLocation> processor) {
        if (schema == null) {
            return;
        }

        if (schema.getPattern() != null) {
            processor.accept(new PatternLocation(currentLocation + "/pattern", schema.getPattern()));
        }

        // Resiliently scan properties
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((propName, propValue) -> {
                if (propValue instanceof Schema) {
                    scanSchemaForPatterns((Schema<?>) propValue, currentLocation + "/properties/" + propName, processor);
                } else {
                    log.warn("Skipping non-schema object found at path: {}/properties/{}", currentLocation, propName);
                }
            });
        }

        // Resiliently scan array items
        if (schema.getItems() != null) {
            if (schema.getItems() instanceof Schema) {
                scanSchemaForPatterns(schema.getItems(), currentLocation + "/items", processor);
            } else {
                log.warn("Skipping non-schema object found at path: {}/items", currentLocation);
            }
        }

        scanComposition(schema.getAllOf(), "allOf", currentLocation, processor);
        scanComposition(schema.getAnyOf(), "anyOf", currentLocation, processor);
        scanComposition(schema.getOneOf(), "oneOf", currentLocation, processor);

        // Resiliently scan additionalProperties
        if (schema.getAdditionalProperties() instanceof Schema) {
            scanSchemaForPatterns((Schema<?>) schema.getAdditionalProperties(), currentLocation + "/additionalProperties", processor);
        }
    }

    private void scanComposition(List<Schema> schemas, String keyword, String currentLocation, Consumer<PatternLocation> processor) {
        if (schemas != null) {
            for (int i = 0; i < schemas.size(); i++) {
                Object item = schemas.get(i);
                if (item instanceof Schema) {
                    scanSchemaForPatterns((Schema<?>) item, currentLocation + "/" + keyword + "/" + i, processor);
                } else {
                    log.warn("Skipping non-schema object in composition list at path: {}/{}/{}", currentLocation, keyword, i);
                }
            }
        }
    }

    private String escapePath(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }

    private record PatternLocation(String path, String pattern) {}
}