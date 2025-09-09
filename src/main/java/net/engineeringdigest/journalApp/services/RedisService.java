package net.engineeringdigest.journalApp.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
        } catch (Exception e) {
            log.error("Error getting value from Redis for key: {}", key, e);
            return null;
        }
    }

    public <T> List<T> getList(String key, TypeReference<List<T>> typeReference) {
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
        } catch (Exception e) {
            log.error("Error getting list from Redis for key: {}", key, e);
            return null;
        }
    }

    public void set(String key, Object value, Long ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl, TimeUnit.SECONDS);
            log.debug("Cached value for key: {} with TTL: {} seconds", key, ttl);
        } catch (Exception e) {
            log.error("Error setting value in Redis for key: {}", key, e);
        }
    }

    public void set(String key, Object value) {
        set(key, value, COLLECTIONS_TTL); // Default TTL
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Deleted cache key: {}", key);
        } catch (Exception e) {
            log.error("Error deleting key from Redis: {}", key, e);
        }
    }

    public void deletePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Error deleting keys with pattern: {}", pattern, e);
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking key existence in Redis: {}", key, e);
            return false;
        }
    }

    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Error setting expiration for key: {}", key, e);
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
}
