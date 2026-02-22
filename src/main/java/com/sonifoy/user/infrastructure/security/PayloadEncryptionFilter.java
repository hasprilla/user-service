package com.sonifoy.user.infrastructure.security;

import com.sonifoy.user.infrastructure.security.crypto.CryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Filter that transparently decrypts incoming request bodies and encrypts
 * outgoing responses
 * using dynamic session keys fetched from Redis.
 */
@Component
@org.springframework.core.annotation.Order(-10)
@RequiredArgsConstructor
public class PayloadEncryptionFilter implements WebFilter {

    private final CryptoService cryptoService;
    private final SessionKeyStore sessionKeyStore;
    private static final String SESSION_ID_HEADER = "X-Session-ID";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PayloadEncryptionFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        log.debug("PayloadEncryptionFilter processing {} request for path: {}", method, path);

        // 1. Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        // 2. Skip specific paths WITHOUT session check (Critical for handshake and
        // auth)
        if (path.contains("/handshake") || path.contains("/actuator") ||
                path.contains("/maintenance") || path.contains("/exploration") ||
                path.contains("/ranking") || path.contains("/auth")) {
            log.debug("Skipping encryption for path: {}", path);
            return chain.filter(exchange);
        }

        String sessionId = exchange.getRequest().getHeaders().getFirst(SESSION_ID_HEADER);
        if (sessionId == null) {
            log.warn("Unauthorized request to {}: Missing X-Session-ID header", path);
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Session-ID"));
        }

        return sessionKeyStore.getKey(sessionId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Unauthorized request to {}: Session ID {} not found or expired", path, sessionId);
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired"));
                }))
                .flatMap(sessionKey -> {
                    log.debug("Session key found for ID: {}. Proceeding with request to {}", sessionId, path);
                    ServerHttpResponse mutatedResponse = decorateResponse(exchange, sessionKey, sessionId);

                    if (isDecryptionRequired(exchange)) {
                        log.debug("Decryption required for {} {}", method, path);
                        return decryptRequestBody(exchange.mutate().response(mutatedResponse).build(), chain,
                                sessionKey, sessionId);
                    }
                    return chain.filter(exchange.mutate().response(mutatedResponse).build());
                });
    }

    private boolean isDecryptionRequired(ServerWebExchange exchange) {
        String method = exchange.getRequest().getMethod().name();
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        return (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) &&
                contentType != null && contentType.isCompatibleWith(MediaType.TEXT_PLAIN);
    }

    private Mono<Void> decryptRequestBody(ServerWebExchange exchange, WebFilterChain chain, byte[] sessionKey,
            String sessionId) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String bodyStr = new String(bytes, StandardCharsets.UTF_8).trim();
                    // Remove potential surrounding quotes from Dio/HttpClient
                    if (bodyStr.startsWith("\"") && bodyStr.endsWith("\"")) {
                        bodyStr = bodyStr.substring(1, bodyStr.length() - 1);
                    }

                    try {
                        String decryptedJson = cryptoService.decrypt(bodyStr, sessionKey);
                        log.info("[PayloadEncryptionFilter] Decrypted body ({} chars)", decryptedJson.length());
                        byte[] decryptedBytes = decryptedJson.getBytes(StandardCharsets.UTF_8);

                        // 1. Create a request with updated headers and body
                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders headers = new HttpHeaders();
                                headers.putAll(super.getHeaders());
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                headers.setContentLength(decryptedBytes.length);
                                return headers;
                            }

                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.just(exchange.getResponse().bufferFactory().wrap(decryptedBytes));
                            }
                        };

                        // 2. Build final exchange and process chain
                        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                                .then(Mono.just(true)); // Emit something to prevent switchIfEmpty
                    } catch (Exception e) {
                        log.error("Decryption failed for session: {}", sessionId, e);
                        return Mono.error(
                                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid encrypted payload"));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).then(Mono.just(true))))
                .then();
    }

    private ServerHttpResponse decorateResponse(ServerWebExchange exchange, byte[] sessionKey, String sessionId) {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                // ONLY encrypt if the status is SUCCESS (2xx)
                // Error responses (4xx, 5xx) should be sent as plain JSON for easier debugging
                // and standard handling
                HttpStatus status = (HttpStatus) getStatusCode();
                if (status != null && !status.is2xxSuccessful()) {
                    return super.writeWith(body);
                }

                MediaType contentType = getHeaders().getContentType();
                if (contentType != null && (contentType.isCompatibleWith(MediaType.APPLICATION_JSON) ||
                        contentType.isCompatibleWith(new MediaType("application", "json", StandardCharsets.UTF_8)))) {

                    return DataBufferUtils.join(Flux.from(body))
                            .flatMap(dataBuffer -> {
                                if (dataBuffer.readableByteCount() == 0) {
                                    getDelegate().getHeaders().setContentLength(0);
                                    return super.writeWith(Mono.empty());
                                }
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);

                                try {
                                    String plainBody = new String(bytes, StandardCharsets.UTF_8);

                                    // Robust check for already encrypted content (IV:Ciphertext)
                                    // Encrypted strings won't start with { or [ which are JSON markers
                                    boolean isProbablyJson = plainBody.trim().startsWith("{")
                                            || plainBody.trim().startsWith("[");

                                    if (!isProbablyJson && plainBody.contains(":")) {
                                        log.debug(
                                                "[PayloadEncryptionFilter] Response seems already encrypted, skipping.");
                                        return super.writeWith(Mono.just(getDelegate().bufferFactory().wrap(bytes)));
                                    }

                                    log.debug("[PayloadEncryptionFilter] Encrypting JSON response ({} bytes)",
                                            bytes.length);
                                    String encrypted = cryptoService.encrypt(plainBody, sessionKey);
                                    byte[] encryptedBytes = encrypted.getBytes(StandardCharsets.UTF_8);

                                    getHeaders().setContentType(MediaType.TEXT_PLAIN);
                                    getHeaders().setContentLength(encryptedBytes.length);

                                    return super.writeWith(
                                            Mono.just(getDelegate().bufferFactory().wrap(encryptedBytes)));
                                } catch (Exception e) {
                                    log.error("Encryption failed for session: {}", sessionId, e);
                                    // Fallback: send as is
                                    return super.writeWith(Mono.just(getDelegate().bufferFactory().wrap(bytes)));
                                }
                            })
                            .switchIfEmpty(super.writeWith(body));
                }
                return super.writeWith(body);
            }
        };
    }
}
