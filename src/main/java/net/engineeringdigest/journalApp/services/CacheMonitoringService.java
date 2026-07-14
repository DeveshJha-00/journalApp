package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class CacheMonitoringService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.redis.cache.enabled:true}")
    private boolean cacheEnabled = true;

    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        if (!cacheEnabled) {
            stats.put("enabled", false);
            stats.put("totalKeys", 0);
            stats.put("keysByType", new HashMap<>());
            return stats;
        }

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

        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable while retrieving cache statistics: {}", e.getMessage());
            stats.put("error", "Redis unavailable");
        } catch (Exception e) {
            log.warn("Error retrieving cache statistics: {}", e.getMessage());
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
        if (!cacheEnabled) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys", keys.size());
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable while clearing cache: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Error clearing cache: {}", e.getMessage());
        }
    }

    public void clearCacheByPattern(String pattern) {
        if (!cacheEnabled) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable while clearing cache with pattern {}: {}", pattern, e.getMessage());
        } catch (Exception e) {
            log.warn("Error clearing cache with pattern {}: {}", pattern, e.getMessage());
        }
    }
}
