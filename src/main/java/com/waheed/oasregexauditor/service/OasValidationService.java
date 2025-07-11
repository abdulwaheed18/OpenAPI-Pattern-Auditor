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

    public List<GroupedValidationResult> validateOasRegex(OpenAPI openAPI,
                                                          boolean validateJava, boolean validateJs, boolean validateGoRe2j,
                                                          boolean qualityCheckPermissive, boolean qualityCheckAnchors, boolean qualityCheckRedos,
                                                          boolean checkNaming, boolean checkOperationId, boolean checkSummary, boolean checkSchemaDescription, boolean checkSchemaExample) {
        List<ValidationResult> flatResults = new ArrayList<>();
        List<RegexValidator> activeValidators = getActiveValidators(validateJava, validateJs, validateGoRe2j);

        Consumer<PatternLocation> patternProcessor = (loc) -> {
            if (!activeValidators.isEmpty()) {
                for (RegexValidator validator : activeValidators) {
                    flatResults.add(validator.validate(loc.path, loc.pattern));
                }
            }
            flatResults.addAll(qualityValidator.validateRegex(loc.path, loc.pattern, qualityCheckPermissive, qualityCheckAnchors, qualityCheckRedos));
        };

        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            openAPI.getComponents().getSchemas().forEach((schemaName, schema) ->
                    scanSchemaForPatterns(schema, "#/components/schemas/" + schemaName, patternProcessor)
            );
        }

        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                // This part is simplified for brevity, the full traversal logic would be here
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

    private List<RegexValidator> getActiveValidators(boolean java, boolean js, boolean go) {
        List<RegexValidator> active = new ArrayList<>();
        for (RegexValidator validator : validators) {
            if (java && "Java".equals(validator.getEngineName())) active.add(validator);
            if (js && "JavaScript".equals(validator.getEngineName())) active.add(validator);
            if (go && "Go (RE2J)".equals(validator.getEngineName())) active.add(validator);
        }
        return active;
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
