package net.engineeringdigest.journalApp.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.redis.cache.enabled:true}")
    private boolean cacheEnabled = true;

    @Value("${app.redis.failure-backoff-ms:60000}")
    private long failureBackoffMs = 60000L;

    private volatile long redisUnavailableUntilMs = 0L;

    // Cache key constants
    public static final String USER_PROFILE_KEY = "user:profile:";
    public static final String USER_COLLECTIONS_KEY = "user:collections:";
    public static final String COLLECTION_KEY = "collection:";
    public static final String USER_RECENT_ENTRIES_KEY = "user:recent_entries:";
    public static final String COLLECTION_ENTRIES_KEY = "collection:entries:";
    public static final String ENTRY_KEY = "entry:";

    // TTL constants (in seconds)
    public static final long USER_PROFILE_TTL = 1800; // 30 minutes
    public static final long COLLECTIONS_TTL = 1800; // 30 minutes
    public static final long ENTRIES_TTL = 900; // 15 minutes
    public static final long RECENT_ENTRIES_TTL = 600; // 10 minutes

    public <T> T get(String key, Class<T> entityClass) {
        if (shouldSkipRedis()) {
            return null;
        }

        try {
            Object o = redisTemplate.opsForValue().get(key);
            if (o == null) {
                return null;
            }

            // Handle different object types returned by Redis
            if (entityClass.isInstance(o)) {
                return entityClass.cast(o);
            } else if (o instanceof String) {
                return objectMapper.readValue((String) o, entityClass);
            } else if (o instanceof java.util.Map) {
                // Convert LinkedHashMap back to the desired object
                return objectMapper.convertValue(o, entityClass);
            } else {
                return objectMapper.readValue(o.toString(), entityClass);
            }
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable("get", key, e);
            return null;
        } catch (Exception e) {
            log.warn("Redis get failed for key {}. Treating as cache miss: {}", key, e.getMessage());
            return null;
        }
    }

    public <T> List<T> getList(String key, TypeReference<List<T>> typeReference) {
        if (shouldSkipRedis()) {
            return null;
        }

        try {
            Object o = redisTemplate.opsForValue().get(key);
            if (o == null) {
                return null;
            }

            if (o instanceof String) {
                return objectMapper.readValue((String) o, typeReference);
            } else if (o instanceof java.util.List) {
                // Convert List of LinkedHashMaps back to the desired objects
                return objectMapper.convertValue(o, typeReference);
            } else {
                return objectMapper.readValue(o.toString(), typeReference);
            }
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable("getList", key, e);
            return null;
        } catch (Exception e) {
            log.warn("Redis list get failed for key {}. Treating as cache miss: {}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, Object value, Long ttl) {
        if (shouldSkipRedis()) {
            return;
        }

        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl, TimeUnit.SECONDS);
            log.debug("Cached value for key: {} with TTL: {} seconds", key, ttl);
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable("set", key, e);
        } catch (Exception e) {
            log.warn("Redis set failed for key {}. Continuing without cache: {}", key, e.getMessage());
        }
    }

    public void set(String key, Object value) {
        set(key, value, COLLECTIONS_TTL); // Default TTL
    }

    public void delete(String key) {
        if (shouldSkipRedis()) {
            return;
        }

        try {
            redisTemplate.delete(key);
            log.debug("Deleted cache key: {}", key);
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable("delete", key, e);
        } catch (Exception e) {
            log.warn("Redis delete failed for key {}. Continuing without cache invalidation: {}", key, e.getMessage());
        }
    }

    public void deletePattern(String pattern) {
        if (shouldSkipRedis()) {
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable("deletePattern", pattern, e);
        } catch (Exception e) {
            log.warn("Redis pattern delete failed for pattern {}. Continuing without cache invalidation: {}", pattern, e.getMessage());
        }
    }

    public boolean exists(String key) {
        if (shouldSkipRedis()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable("exists", key, e);
            return false;
        } catch (Exception e) {
            log.warn("Redis exists check failed for key {}. Treating as absent: {}", key, e.getMessage());
            return false;
        }
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        if (shouldSkipRedis()) {
            return;
        }

        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (RedisConnectionFailureException e) {
            markRedisUnavailable("expire", key, e);
        } catch (Exception e) {
            log.warn("Redis expire failed for key {}. Continuing without expiration update: {}", key, e.getMessage());
        }
    }

    // Specialized caching methods
    public String buildUserProfileKey(String userId) {
        return USER_PROFILE_KEY + userId;
    }

    public String buildUserCollectionsKey(String userId) {
        return USER_COLLECTIONS_KEY + userId;
    }

    public String buildCollectionKey(String collectionId) {
        return COLLECTION_KEY + collectionId;
    }

    public String buildUserRecentEntriesKey(String userId) {
        return USER_RECENT_ENTRIES_KEY + userId;
    }

    public String buildCollectionEntriesKey(String collectionId) {
        return COLLECTION_ENTRIES_KEY + collectionId;
    }

    public String buildEntryKey(String entryId) {
        return ENTRY_KEY + entryId;
    }

    public void invalidateUserCache(String userId) {
        deletePattern(USER_PROFILE_KEY + userId + "*");
        deletePattern(USER_COLLECTIONS_KEY + userId + "*");
        deletePattern(USER_RECENT_ENTRIES_KEY + userId + "*");
        log.info("Invalidated all cache for user: {}", userId);
    }

    public void invalidateCollectionCache(String collectionId) {
        delete(buildCollectionKey(collectionId));
        delete(buildCollectionEntriesKey(collectionId));
        log.info("Invalidated cache for collection: {}", collectionId);
    }

    private boolean shouldSkipRedis() {
        return !cacheEnabled || System.currentTimeMillis() < redisUnavailableUntilMs;
    }

    private void markRedisUnavailable(String operation, String key, Exception e) {
        redisUnavailableUntilMs = System.currentTimeMillis() + failureBackoffMs;
        log.warn("Redis unavailable during {} for key '{}'. Cache will be skipped for {} ms. Cause: {}",
                operation, key, failureBackoffMs, e.getMessage());
    }
}
