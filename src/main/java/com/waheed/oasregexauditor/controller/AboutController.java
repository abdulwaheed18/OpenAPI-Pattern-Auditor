package com.waheed.oasregexauditor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle requests for static pages like the 'About' page.
 */
@Controller
@RequestMapping("/openapiauditor")
public class AboutController {

    /**
     * Displays the about page.
     *
     * @return The name of the Thymeleaf template for the about page.
     */
    @GetMapping("/about")
    public String showAboutPage() {
        return "about";
    }
}