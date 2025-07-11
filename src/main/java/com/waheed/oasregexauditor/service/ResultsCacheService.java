package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.ValidationResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple in-memory cache to store validation results for sharing.
 * In a production environment, this could be replaced with a more robust cache
 * like Caffeine, EhCache, or a distributed cache like Redis.
 */
@Service
public class ResultsCacheService {

    private final Map<String, List<ValidationResult>> cache = new ConcurrentHashMap<>();

    /**
     * Stores a list of validation results with a unique ID.
     * @param id The unique identifier for the results.
     * @param results The list of validation results to store.
     */
    public void store(String id, List<ValidationResult> results) {
        cache.put(id, results);
    }

    /**
     * Retrieves a list of validation results by its ID.
     * @param id The unique identifier.
     * @return An Optional containing the list of results if found, otherwise an empty Optional.
     */
    public Optional<List<ValidationResult>> get(String id) {
        return Optional.ofNullable(cache.get(id));
    }
}