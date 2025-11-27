package com.taskmgmt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Cache management service.
 * Handles cache invalidation across the application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    private final CacheManager cacheManager;
    
    public void evictCache(String cacheName, String key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted cache: {} with key: {}", cacheName, key);
        }
    }
    
    public void evictAll(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.debug("Cleared cache: {}", cacheName);
        }
    }
}

