package com.waheed.oasregexauditor.controller;

import com.waheed.oasregexauditor.model.ValidationResult;
import com.waheed.oasregexauditor.service.OasValidationService;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class OasUploadController {

    @Autowired
    private OasValidationService oasValidationService;

    /**
     * Handles GET requests to display the file upload form.
     * @param model The Spring UI model to pass attributes to the view.
     * @return The name of the Thymeleaf template for the upload page.
     */
    @GetMapping("/")
    public String showUploadForm(Model model) {
        // Add an empty list for results initially, so the table doesn't break on first load
        model.addAttribute("results", List.of());
        return "upload";
    }

    /**
     * Handles POST requests for file uploads and regex validation.
     * @param file The uploaded OpenAPI Specification file.
     * @param validateJava Boolean indicating if Java regex validation should be performed.
     * @param validateJs Boolean indicating if JavaScript regex validation should be performed.
     * @param validateGoRe2j Boolean indicating if Go (RE2J) regex validation should be performed.
     * @param model The Spring UI model to pass results to the view.
     * @return The name of the Thymeleaf template to display the results.
     */
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("oasFile") MultipartFile file,
                                   @RequestParam(value = "validateJava", defaultValue = "false") boolean validateJava,
                                   @RequestParam(value = "validateJs", defaultValue = "false") boolean validateJs,
                                   @RequestParam(value = "validateGoRe2j", defaultValue = "false") boolean validateGoRe2j,
                                   Model model) {
        if (file.isEmpty()) {
            model.addAttribute("message", "Please select an OAS file to upload.");
            model.addAttribute("results", List.of()); // Ensure results list is empty
            return "upload";
        }

        try {
            // Read file content
            String oasContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // Parse the OpenAPI content
            ParseOptions options = new ParseOptions();
            options.setResolve(true); // Resolve external references
            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(oasContent, null, options);

            if (parseResult.getOpenAPI() == null) {
                model.addAttribute("message", "Failed to parse OpenAPI file. Errors: " + parseResult.getMessages());
                model.addAttribute("results", List.of());
                return "upload";
            }

            OpenAPI openAPI = parseResult.getOpenAPI();

            // Perform validation based on selected engines
            List<ValidationResult> results = oasValidationService.validateOasRegex(
                    openAPI, validateJava, validateJs, validateGoRe2j);

            model.addAttribute("message", "OAS file processed successfully!");
            model.addAttribute("results", results);

        } catch (IOException e) {
            model.addAttribute("message", "Error reading file: " + e.getMessage());
            model.addAttribute("results", List.of());
        } catch (Exception e) {
            model.addAttribute("message", "An unexpected error occurred: " + e.getMessage());
            model.addAttribute("results", List.of());
            e.printStackTrace(); // Log the full stack trace for debugging
        }
        return "upload";
    }
}