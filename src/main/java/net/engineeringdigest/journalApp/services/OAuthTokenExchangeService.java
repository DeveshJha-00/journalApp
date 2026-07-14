package net.engineeringdigest.journalApp.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuthTokenExchangeService {

    private static final String KEY_PREFIX = "oauth:code:";
    private static final long CODE_TTL_SECONDS = 120L;

    @Autowired
    private RedisService redisService;

    private final Map<String, LocalToken> localFallback = new ConcurrentHashMap<>();

    public String createCode(String jwt) {
        String code = UUID.randomUUID().toString();
        String key = KEY_PREFIX + code;
        redisService.set(key, jwt, CODE_TTL_SECONDS);
        localFallback.put(code, new LocalToken(jwt, Instant.now().plusSeconds(CODE_TTL_SECONDS)));
        return code;
    }

    public String consumeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        String key = KEY_PREFIX + code;
        String jwt = redisService.get(key, String.class);
        redisService.delete(key);

        LocalToken localToken = localFallback.remove(code);
        if (jwt != null) {
            return jwt;
        }
        if (localToken == null || localToken.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }
        return localToken.getJwt();
    }

    @Data
    @AllArgsConstructor
    private static class LocalToken {
        private String jwt;
        private Instant expiresAt;
    }
}
