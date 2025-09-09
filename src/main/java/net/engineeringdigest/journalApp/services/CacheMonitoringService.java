package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class CacheMonitoringService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get total number of keys
            Set<String> allKeys = redisTemplate.keys("*");
            stats.put("totalKeys", allKeys != null ? allKeys.size() : 0);

            // Count keys by type
            Map<String, Integer> keysByType = new HashMap<>();
            if (allKeys != null) {
                for (String key : allKeys) {
                    String type = getKeyType(key);
                    keysByType.put(type, keysByType.getOrDefault(type, 0) + 1);
                }
            }
            stats.put("keysByType", keysByType);

            // Get memory usage info (if available)
            try {
                Object memoryInfo = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    return connection.info("memory");
                });
                stats.put("memoryInfo", memoryInfo);
            } catch (Exception e) {
                log.debug("Could not retrieve memory info: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error retrieving cache statistics", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    private String getKeyType(String key) {
        if (key.startsWith(RedisService.USER_PROFILE_KEY)) {
            return "userProfiles";
        } else if (key.startsWith(RedisService.USER_COLLECTIONS_KEY)) {
            return "userCollections";
        } else if (key.startsWith(RedisService.COLLECTION_KEY)) {
            return "collections";
        } else if (key.startsWith(RedisService.USER_RECENT_ENTRIES_KEY)) {
            return "recentEntries";
        } else if (key.startsWith(RedisService.COLLECTION_ENTRIES_KEY)) {
            return "collectionEntries";
        } else if (key.startsWith(RedisService.ENTRY_KEY)) {
            return "journalEntries";
        } else {
            return "other";
        }
    }

    public void clearAllCache() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing cache", e);
        }
    }

    public void clearCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error clearing cache with pattern: {}", pattern, e);
        }
    }
}