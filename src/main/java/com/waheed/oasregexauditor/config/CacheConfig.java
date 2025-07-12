package com.waheed.oasregexauditor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration class for setting up the application's cache.
 * Enables Spring's caching abstraction and configures Caffeine as the cache provider.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures the CacheManager bean for the application.
     * This setup uses Caffeine and defines a cache named "analysisResults"
     * with a time-to-live (TTL) of 1 hour and a maximum size of 500 entries.
     *
     * @return A configured CaffeineCacheManager instance.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("analysisResults");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Evict entries from the cache 1 hour after they were last written.
                .expireAfterWrite(1, TimeUnit.HOURS)
                // Limit the cache size to a maximum of 500 entries.
                .maximumSize(500)
        );
        return cacheManager;
    }
}
