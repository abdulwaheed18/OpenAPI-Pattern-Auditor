package com.waheed.oasregexauditor.controller;

import com.waheed.oasregexauditor.config.AppConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * A Controller Advice to add globally available attributes to the model.
 * This makes the 'appConfig' object accessible in all Thymeleaf templates
 * without needing to add it in every controller method.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private final AppConfigProperties appConfigProperties;

    @Autowired
    public GlobalControllerAdvice(AppConfigProperties appConfigProperties) {
        this.appConfigProperties = appConfigProperties;
    }

    /**
     * Adds the application configuration properties to the model under the key "appConfig".
     * @return the application configuration properties.
     */
    @ModelAttribute("appConfig")
    public AppConfigProperties getAppConfigProperties() {
        return appConfigProperties;
    }
}
