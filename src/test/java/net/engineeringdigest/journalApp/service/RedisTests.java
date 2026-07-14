package net.engineeringdigest.journalApp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.engineeringdigest.journalApp.services.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTests {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisService redisService;

    @Test
    void get_shouldReturnDeserializedObject_whenStoredAsString() throws Exception {

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test:key")).thenReturn("{\"name\":\"ashu\"}");
        when(objectMapper.readValue("{\"name\":\"ashu\"}", Dummy.class))
                .thenReturn(new Dummy("ashu"));

        Dummy result = redisService.get("test:key", Dummy.class);

        assertEquals("ashu", result.getName());
    }

    @Test
    void set_shouldStoreSerializedValueWithTTL() throws Exception {

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"name\":\"ashu\"}");

        redisService.set("test:key", new Dummy("ashu"), 60L);

        verify(valueOperations)
                .set(eq("test:key"), eq("{\"name\":\"ashu\"}"), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void exists_shouldReturnTrue_whenKeyExists() {
        when(redisTemplate.hasKey("key")).thenReturn(true);

        assertTrue(redisService.exists("key"));
    }

    @Test
    void delete_shouldCallRedisDelete() {
        redisService.delete("key");
        verify(redisTemplate).delete("key");
    }

    @Test
    void buildUserProfileKey_shouldAppendPrefix() {
        assertEquals("user:profile:123", redisService.buildUserProfileKey("123"));
    }

    @Test
    void get_shouldSkipRedisTemporarily_afterConnectionFailure() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test:key"))
                .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        assertNull(redisService.get("test:key", Dummy.class));
        assertNull(redisService.get("test:key", Dummy.class));

        verify(valueOperations, times(1)).get("test:key");
    }

    static class Dummy {
        private String name;
        public Dummy() {}
        public Dummy(String name) { this.name = name; }
        public String getName() { return name; }
    }
}
