package net.engineeringdigest.journalApp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bson.types.ObjectId;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.IOException;
import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper()));
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL of 30 minutes
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())));

        // Different TTL configurations for different cache types
        RedisCacheConfiguration userConfig = defaultConfig.entryTtl(Duration.ofMinutes(30));
        RedisCacheConfiguration collectionsConfig = defaultConfig.entryTtl(Duration.ofMinutes(30));
        RedisCacheConfiguration entriesConfig = defaultConfig.entryTtl(Duration.ofMinutes(15));
        RedisCacheConfiguration recentEntriesConfig = defaultConfig.entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("users", userConfig)
                .withCacheConfiguration("userCollections", collectionsConfig)
                .withCacheConfiguration("collections", collectionsConfig)
                .withCacheConfiguration("journalEntries", entriesConfig)
                .withCacheConfiguration("collectionEntries", entriesConfig)
                .withCacheConfiguration("recentEntries", recentEntriesConfig)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        // Add ObjectId serialization support
        SimpleModule objectIdModule = new SimpleModule();
        objectIdModule.addSerializer(ObjectId.class, new ObjectIdSerializer());
        objectIdModule.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
        mapper.registerModule(objectIdModule);

        mapper.findAndRegisterModules();
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Custom ObjectId serializer to handle MongoDB ObjectId properly
     */
    public static class ObjectIdSerializer extends JsonSerializer<ObjectId> {
        @Override
        public void serialize(ObjectId objectId, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (objectId == null) {
                jsonGenerator.writeNull();
            } else {
                jsonGenerator.writeString(objectId.toHexString());
            }
        }
    }

    /**
     * Custom ObjectId deserializer to handle MongoDB ObjectId properly
     */
    public static class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {
        @Override
        public ObjectId deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            String value = jsonParser.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new ObjectId(value);
        }
    }
}
