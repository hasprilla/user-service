package com.sonifoy.user.infrastructure.security;

import reactor.core.publisher.Mono;

public interface SessionKeyStore {
    Mono<Void> saveKey(String sessionId, byte[] key);

    Mono<byte[]> getKey(String sessionId);

    Mono<Void> removeKey(String sessionId);
}
