package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.GroupedValidationResult;
import com.waheed.oasregexauditor.model.ValidationResult;
import com.waheed.oasregexauditor.service.validators.PatternQualityValidator;
import com.waheed.oasregexauditor.service.validators.RegexValidator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
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

    private final List<RegexValidator> validators;
    private final PatternQualityValidator qualityValidator;

    @Autowired
    public OasValidationService(List<RegexValidator> validators, PatternQualityValidator qualityValidator) {
        this.validators = validators;
        this.qualityValidator = qualityValidator;
    }

    public List<GroupedValidationResult> validateOas(OpenAPI openAPI, String oasContent, String engine,
                                                     boolean qualityCheckPermissive, boolean qualityCheckAnchors, boolean qualityCheckRedos,
                                                     boolean checkNaming, boolean checkOperationId, boolean checkSummary,
                                                     boolean checkSchemaDescription, boolean checkSchemaExample) {
        List<ValidationResult> flatResults = new ArrayList<>();
        Optional<RegexValidator> activeValidator = getActiveValidator(engine);

        // 1. Validate Regex patterns
        Consumer<PatternLocation> patternProcessor = loc -> {
            int lineNumber = findLineNumber(oasContent, loc.pattern());
            activeValidator.ifPresent(validator -> flatResults.add(validator.validate(loc.path(), lineNumber, loc.pattern())));
            flatResults.addAll(qualityValidator.validateRegex(loc.path(), lineNumber, loc.pattern(), qualityCheckPermissive, qualityCheckAnchors, qualityCheckRedos));
        };
        scanForPatterns(openAPI, patternProcessor);

        // 2. Validate Best Practices
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                String pathLocation = "#/paths/" + escapePath(path);
                flatResults.addAll(qualityValidator.validatePath(pathLocation, path, checkNaming));
                if (pathItem.readOperations() != null) {
                    pathItem.readOperations().forEach(operation -> {
                        String opLocation = pathLocation + "/" + pathItem.readOperationsMap().entrySet().stream()
                                .filter(e -> e.getValue().equals(operation)).findFirst().get().getKey();
                        flatResults.addAll(qualityValidator.validateOperation(opLocation, operation, checkOperationId, checkSummary));
                    });
                }
            });
        }
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            openAPI.getComponents().getSchemas().forEach((schemaName, schema) -> {
                String schemaLocation = "#/components/schemas/" + schemaName;
                flatResults.addAll(qualityValidator.validateSchema(schemaLocation, schema, checkSchemaDescription, checkSchemaExample));
            });
        }

        // Group results by location and then by the specific regex pattern
        Map<String, Map<String, List<ValidationResult>>> groupedByLocationAndPattern = flatResults.stream()
                .collect(Collectors.groupingBy(
                        ValidationResult::getLocation,
                        Collectors.groupingBy(res -> res.getRegexPattern() != null ? res.getRegexPattern() : "N/A")
                ));

        List<GroupedValidationResult> finalResults = new ArrayList<>();
        groupedByLocationAndPattern.forEach((location, patternMap) -> {
            patternMap.forEach((pattern, results) -> {
                if (!results.isEmpty()) {
                    // Get the line number from the first result in the group that has one.
                    int lineNumber = results.stream()
                            .mapToInt(ValidationResult::getLineNumber)
                            .filter(ln -> ln > 0)
                            .findFirst()
                            .orElse(0); // Default to 0 if no result has a line number (e.g., best practice issues)
                    finalResults.add(new GroupedValidationResult(location, lineNumber, pattern, results));
                }
            });
        });

        return finalResults;
    }

    private void scanForPatterns(OpenAPI openAPI, Consumer<PatternLocation> processor) {
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            openAPI.getComponents().getSchemas().forEach((schemaName, schema) ->
                    scanSchemaForPatterns(schema, "#/components/schemas/" + schemaName, processor)
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
                                    String location = String.format("#/paths/%s/%s/parameters/%d",
                                            escapePath(path),
                                            pathItem.readOperationsMap().entrySet().stream().filter(e -> e.getValue().equals(operation)).findFirst().get().getKey(),
                                            i);
                                    processor.accept(new PatternLocation(location, param.getSchema().getPattern()));
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    private void scanSchemaForPatterns(Schema<?> schema, String currentLocation, Consumer<PatternLocation> processor) {
        if (schema == null) return;
        if (schema.getPattern() != null) {
            processor.accept(new PatternLocation(currentLocation, schema.getPattern()));
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

    private int findLineNumber(String content, String pattern) {
        String[] lines = content.split("\r\n|\n");
        // Create more robust search strings, ignoring potential extra spaces
        String searchString1 = "pattern: '" + pattern + "'";
        String searchString2 = "pattern: \"" + pattern + "\"";
        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim().replaceAll("\\s+", " ");
            if (trimmedLine.contains(searchString1) || trimmedLine.contains(searchString2)) {
                return i + 1;
            }
        }
        return 0;
    }

    private Optional<RegexValidator> getActiveValidator(String engine) {
        return validators.stream()
                .filter(v -> v.getEngineName().toLowerCase().startsWith(engine.toLowerCase()))
                .findFirst();
    }

    private String escapePath(String path) {
        return path.replace("~", "~0").replace("/", "~1");
    }

    private record PatternLocation(String path, String pattern) {}
}