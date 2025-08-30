package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.services.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


public class RedisTests {

    @Mock
   private RedisTemplate<String, String> redisTemplate;

   @Mock
   private ValueOperations <String, String> valueOperations;

    @InjectMocks
    private RedisService myRedisService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Disabled("Disabled for CI/CD - mocked test, no real Redis call")
    @Test
    void testRedisGet() {
        // arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("name")).thenReturn("mockedAshu");

        // act
        String name = redisTemplate.opsForValue().get("name");

        // assert
        assertEquals("mockedAshu", name);
        System.out.println("Name from mocked Redis: " + name);
    }
}
