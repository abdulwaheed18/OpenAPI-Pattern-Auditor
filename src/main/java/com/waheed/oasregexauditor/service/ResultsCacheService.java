package com.waheed.oasregexauditor.service;

import com.waheed.oasregexauditor.model.GroupedValidationResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ResultsCacheService {
    private final Map<String, List<GroupedValidationResult>> cache = new ConcurrentHashMap<>();

    public void store(String id, List<GroupedValidationResult> results) {
        cache.put(id, results);
    }

    public Optional<List<GroupedValidationResult>> get(String id) {
        return Optional.ofNullable(cache.get(id));
    }
}
