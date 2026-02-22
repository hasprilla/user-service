package com.sonifoy.user.infrastructure.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;

@Service
public class RedisSessionKeyStore implements SessionKeyStore {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RedisSessionKeyStore(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String KEY_PREFIX = "session:key:";
    private static final Duration KEY_TTL = Duration.ofHours(24);

    public Mono<Void> saveKey(String sessionId, byte[] key) {
        String encodedKey = Base64.getEncoder().encodeToString(key);
        return redisTemplate.opsForValue()
                .set(KEY_PREFIX + sessionId, encodedKey, KEY_TTL)
                .then();
    }

    public Mono<byte[]> getKey(String sessionId) {
        return redisTemplate.opsForValue()
                .get(KEY_PREFIX + sessionId)
                .map(encodedKey -> Base64.getDecoder().decode(encodedKey));
    }

    public Mono<Void> removeKey(String sessionId) {
        return redisTemplate.opsForValue()
                .delete(KEY_PREFIX + sessionId)
                .then();
    }
}
