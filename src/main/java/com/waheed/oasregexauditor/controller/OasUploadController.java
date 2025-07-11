package com.waheed.oasregexauditor.controller;

import com.waheed.oasregexauditor.model.ValidationResult;
import com.waheed.oasregexauditor.service.OasValidationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Controller for handling file uploads and displaying validation results.
 */
@Controller
public class OasUploadController {

    private static final Logger log = LoggerFactory.getLogger(OasUploadController.class);

    @Autowired
    private OasValidationService oasValidationService;

    @GetMapping("/")
    public String showUploadForm(Model model) {
        model.addAttribute("results", Collections.emptyList());
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("oasFile") MultipartFile file,
                                   // Engine Syntax Checks
                                   @RequestParam(value = "validateJava", defaultValue = "false") boolean validateJava,
                                   @RequestParam(value = "validateJs", defaultValue = "false") boolean validateJs,
                                   @RequestParam(value = "validateGoRe2j", defaultValue = "false") boolean validateGoRe2j,
                                   // Quality & Security Checks
                                   @RequestParam(value = "qualityCheckPermissive", defaultValue = "false") boolean qualityCheckPermissive,
                                   @RequestParam(value = "qualityCheckAnchors", defaultValue = "false") boolean qualityCheckAnchors,
                                   @RequestParam(value = "qualityCheckRedos", defaultValue = "false") boolean qualityCheckRedos,
                                   Model model) {
        if (file.isEmpty()) {
            model.addAttribute("message", "Error: Please select an OpenAPI file to upload.");
            model.addAttribute("results", Collections.emptyList());
            return "upload";
        }

        log.info("Received file: {}. Size: {} bytes.", file.getOriginalFilename(), file.getSize());

        try {
            String oasContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            ParseOptions options = new ParseOptions();
            options.setResolve(true);

            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(oasContent, null, options);

            OpenAPI openAPI = parseResult.getOpenAPI();
            if (openAPI == null) {
                String errorMessage = "Failed to parse OpenAPI file. Errors: " + String.join(", ", parseResult.getMessages());
                log.error(errorMessage);
                model.addAttribute("message", "Error: " + errorMessage);
                model.addAttribute("results", Collections.emptyList());
                return "upload";
            }

            // Perform validation with all flags
            List<ValidationResult> results = oasValidationService.validateOasRegex(
                    openAPI,
                    validateJava, validateJs, validateGoRe2j,
                    qualityCheckPermissive, qualityCheckAnchors, qualityCheckRedos
            );

            model.addAttribute("message", "Analysis complete for " + file.getOriginalFilename());
            model.addAttribute("results", results);
            log.info("Validation complete. Found {} results.", results.size());

        } catch (IOException e) {
            log.error("Error reading file.", e);
            model.addAttribute("message", "Error: Could not read the uploaded file. " + e.getMessage());
            model.addAttribute("results", Collections.emptyList());
        } catch (Exception e) {
            log.error("An unexpected error occurred during processing.", e);
            model.addAttribute("message", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("results", Collections.emptyList());
        }

        return "upload";
    }
}
