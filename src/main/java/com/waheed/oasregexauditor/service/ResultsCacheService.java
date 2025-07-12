package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.GroupedValidationResult;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service to cache and retrieve analysis results.
 * Uses Spring's caching abstraction with Caffeine.
 */
@Service
public class ResultsCacheService {

    /**
     * Stores the analysis results in the "analysisResults" cache.
     * The method will always be executed and its result placed into the cache.
     *
     * @param id      The unique ID (key) for the cache entry.
     * @param results The list of results to store.
     * @return The stored list of results.
     */
    @CachePut(value = "analysisResults", key = "#id")
    public List<GroupedValidationResult> store(String id, List<GroupedValidationResult> results) {
        // With @CachePut, this method's return value updates the cache.
        // The actual method body can be minimal if no other logic is needed.
        return results;
    }

    /**
     * Retrieves analysis results from the "analysisResults" cache.
     * If the results for the given ID are found in the cache, they are returned directly.
     * If not, the method returns an empty Optional, as there's no backing store to fetch from.
     *
     * @param id The unique ID of the cached result.
     * @return An Optional containing the list of results if found, otherwise an empty Optional.
     */
    @Cacheable(value = "analysisResults", key = "#id")
    public Optional<List<GroupedValidationResult>> get(String id) {
        // With @Cacheable, Spring handles the cache lookup.
        // This method body will only be executed if the item is not in the cache.
        // Since we don't have a "source of truth" to build the cache from here,
        // we return Optional.empty(). The cache should be populated via the store() method.
        return Optional.empty();
    }
}
