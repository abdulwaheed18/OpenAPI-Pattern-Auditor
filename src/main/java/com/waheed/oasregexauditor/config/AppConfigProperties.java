package com.waheed.oasregexauditor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Maps the custom application properties from application.properties into a Java object.
 * The prefix 'app.author' is used to group all related properties.
 */
@Component
@ConfigurationProperties(prefix = "app.author")
@Data // Lombok annotation to generate getters, setters, etc.
public class AppConfigProperties {

    private String name;
    private String email;
    private String title;
    private String github;
    private String linkedin;
    private String blog;
    private String githubRepo;

}
