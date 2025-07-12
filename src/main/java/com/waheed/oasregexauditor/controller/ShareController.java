package com.waheed.oasregexauditor.controller;

import com.waheed.oasregexauditor.model.GroupedValidationResult;
import com.waheed.oasregexauditor.model.Statistics;
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
        // This now correctly expects Optional<List<GroupedValidationResult>>
        Optional<List<GroupedValidationResult>> resultsOptional = resultsCacheService.get(id);

        if (resultsOptional.isPresent() && !resultsOptional.get().isEmpty()) {
            List<GroupedValidationResult> results = resultsOptional.get();
            // **FIX**: Generate statistics from the retrieved results
            Statistics stats = Statistics.fromResults(results);

            model.addAttribute("results", results);
            // **FIX**: Add the statistics object to the model
            model.addAttribute("stats", stats);
            model.addAttribute("message", "Showing shared analysis results from a permalink.");
            // Also add the shareable link so it can be re-shared
            model.addAttribute("shareableLink", true); // A simple flag to show the share section
        } else {
            model.addAttribute("results", Collections.emptyList());
            model.addAttribute("message", "Error: The requested analysis results were not found. The link may be invalid or the results may have expired.");
        }

        // Render the dedicated results page.
        return "results-page";
    }
}
