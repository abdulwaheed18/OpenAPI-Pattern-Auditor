package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.GroupedValidationResult;
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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    // **MODIFIED**: Method signature updated to accept a single engine string
    public List<GroupedValidationResult> validateOasRegex(OpenAPI openAPI,
                                                          String engine,
                                                          boolean qualityCheckPermissive, boolean qualityCheckAnchors, boolean qualityCheckRedos,
                                                          boolean checkNaming, boolean checkOperationId, boolean checkSummary, boolean checkSchemaDescription, boolean checkSchemaExample) {
        List<ValidationResult> flatResults = new ArrayList<>();
        // **MODIFIED**: Get a single optional validator based on the engine string
        Optional<RegexValidator> activeValidator = getActiveValidator(engine);

        Consumer<PatternLocation> patternProcessor = (loc) -> {
            // **MODIFIED**: If the validator is present, use it.
            activeValidator.ifPresent(validator -> flatResults.add(validator.validate(loc.path, loc.pattern)));
            flatResults.addAll(qualityValidator.validateRegex(loc.path, loc.pattern, qualityCheckPermissive, qualityCheckAnchors, qualityCheckRedos));
        };

        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            openAPI.getComponents().getSchemas().forEach((schemaName, schema) ->
                    scanSchemaForPatterns(schema, "#/components/schemas/" + schemaName, patternProcessor)
            );
        }

        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                if (pathItem.readOperations() != null) {
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getParameters() != null) {
                            for (int i = 0; i < operation.getParameters().size(); i++) {
                                Parameter param = operation.getParameters().get(i);
                                if (param.getSchema() != null && param.getSchema().getPattern() != null) {
                                    String location = "#/paths/" + escapePath(path) + "/get/parameters/" + i;
                                    patternProcessor.accept(new PatternLocation(location, param.getSchema().getPattern()));
                                }
                            }
                        }
                    });
                }
            });
        }

        Map<String, List<ValidationResult>> groupedByLocation = flatResults.stream()
                .collect(Collectors.groupingBy(ValidationResult::getLocation));

        return groupedByLocation.entrySet().stream()
                .map(entry -> {
                    String location = entry.getKey();
                    String pattern = entry.getValue().get(0).getRegexPattern();
                    return new GroupedValidationResult(location, pattern, entry.getValue());
                })
                .collect(Collectors.toList());
    }

    // **MODIFIED**: This method now finds a single validator
    private Optional<RegexValidator> getActiveValidator(String engine) {
        return validators.stream()
                .filter(v -> {
                    switch (engine.toLowerCase()) {
                        case "java":
                            return "Java".equals(v.getEngineName());
                        case "js":
                            return "JavaScript".equals(v.getEngineName());
                        case "go":
                            return "Go (RE2J)".equals(v.getEngineName());
                        default:
                            return false;
                    }
                })
                .findFirst();
    }

    private void scanSchemaForPatterns(Schema<?> schema, String currentLocation, Consumer<PatternLocation> processor) {
        if (schema == null) return;
        if (schema.getPattern() != null) {
            processor.accept(new PatternLocation(currentLocation + "/pattern", schema.getPattern()));
        }
        if (schema.getProperties() != null) {
            schema.getProperties().forEach((propName, propSchema) ->
                    scanSchemaForPatterns(propSchema, currentLocation + "/properties/" + propName, processor)
            );
        }
        if (schema.getItems() != null) {
            scanSchemaForPatterns(schema.getItems(), currentLocation + "/items", processor);
        }
    }

    private String escapePath(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }

    private record PatternLocation(String path, String pattern) {}
}
