package com.waheed.oasregexauditor.controller;

import com.waheed.oasregexauditor.model.ValidationResult;
import com.waheed.oasregexauditor.service.ResultsCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Controller to handle displaying shared analysis results via permalinks.
 */
@Controller
public class ShareController {

    @Autowired
    private ResultsCacheService resultsCacheService;

    /**
     * Handles GET requests for a shared result set.
     * @param id The unique ID of the cached result.
     * @param model The Spring UI model.
     * @return The name of the view to render.
     */
    @GetMapping("/r/{id}")
    public String showSharedResults(@PathVariable String id, Model model) {
        Optional<List<ValidationResult>> resultsOptional = resultsCacheService.get(id);

        if (resultsOptional.isPresent()) {
            model.addAttribute("results", resultsOptional.get());
            model.addAttribute("message", "Showing shared analysis results from a permalink.");
        } else {
            model.addAttribute("results", Collections.emptyList());
            model.addAttribute("message", "Error: The requested analysis results were not found. The link may be invalid or the results may have expired.");
        }

        // Render the dedicated results page.
        return "results-page";
    }
}
