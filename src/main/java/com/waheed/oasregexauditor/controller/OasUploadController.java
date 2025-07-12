package com.waheed.oasregexauditor.controller;

import com.waheed.oasregexauditor.model.GroupedValidationResult;
import com.waheed.oasregexauditor.model.Statistics;
import com.waheed.oasregexauditor.service.OasValidationService;
import com.waheed.oasregexauditor.service.ResultsCacheService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/openapiauditor")
public class OasUploadController {

    private static final Logger log = LoggerFactory.getLogger(OasUploadController.class);

    @Autowired
    private OasValidationService oasValidationService;

    @Autowired
    private ResultsCacheService resultsCacheService;

    @GetMapping("")
    public String showUploadForm(Model model) {
        model.addAttribute("initialView", true);
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam(value = "oasFile", required = false) MultipartFile file,
                                   @RequestParam(value = "oasContent", required = false) String oasContent,
                                   @RequestParam(value = "engine", defaultValue = "java") String engine,
                                   @RequestParam(value = "qualityCheckPermissive", defaultValue = "false") boolean qualityCheckPermissive,
                                   @RequestParam(value = "qualityCheckAnchors", defaultValue = "false") boolean qualityCheckAnchors,
                                   @RequestParam(value = "qualityCheckRedos", defaultValue = "false") boolean qualityCheckRedos,
                                   @RequestParam(value = "checkNaming", defaultValue = "false") boolean checkNaming,
                                   @RequestParam(value = "checkOperationId", defaultValue = "false") boolean checkOperationId,
                                   @RequestParam(value = "checkSummary", defaultValue = "false") boolean checkSummary,
                                   @RequestParam(value = "checkSchemaDescription", defaultValue = "false") boolean checkSchemaDescription,
                                   @RequestParam(value = "checkSchemaExample", defaultValue = "false") boolean checkSchemaExample,
                                   Model model) {
        String content;
        String fileName = "pasted-content.yaml";

        try {
            if (oasContent != null && !oasContent.isBlank()) {
                content = oasContent;
            } else if (file != null && !file.isEmpty()) {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
                fileName = file.getOriginalFilename();
            } else {
                model.addAttribute("message", "Error: Please either upload an OpenAPI file or paste its content.");
                model.addAttribute("results", Collections.emptyList());
                return "fragments/results :: results-content";
            }

            ParseOptions options = new ParseOptions();
            options.setResolve(true);
            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(content, null, options);
            OpenAPI openAPI = parseResult.getOpenAPI();

            if (openAPI == null) {
                String errorMessage = "Failed to parse OpenAPI file. Errors: " + String.join(", ", parseResult.getMessages());
                log.error(errorMessage);
                model.addAttribute("message", "Error: " + errorMessage);
                model.addAttribute("results", Collections.emptyList());
                return "fragments/results :: results-content";
            }

            List<GroupedValidationResult> results = oasValidationService.validateOas(
                    openAPI, content, engine,
                    qualityCheckPermissive, qualityCheckAnchors, qualityCheckRedos,
                    checkNaming, checkOperationId, checkSummary, checkSchemaDescription, checkSchemaExample);

            String resultsId = UUID.randomUUID().toString().substring(0, 8);
            resultsCacheService.store(resultsId, results);
            String shareableLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/openapiauditor/r/{id}")
                    .buildAndExpand(resultsId)
                    .toUriString();

            Statistics stats = Statistics.fromResults(results);
            model.addAttribute("stats", stats);

            model.addAttribute("message", "Analysis complete for " + fileName);
            model.addAttribute("results", results);
            model.addAttribute("shareableLink", shareableLink);

        } catch (IOException e) {
            log.error("Error reading file.", e);
            model.addAttribute("message", "Error: Could not read the uploaded file. " + e.getMessage());
            model.addAttribute("results", Collections.emptyList());
        } catch (Exception e) {
            log.error("An unexpected error occurred during processing.", e);
            model.addAttribute("message", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("results", Collections.emptyList());
        }

        model.addAttribute("initialView", false);
        return "fragments/results :: results-content";
    }
}