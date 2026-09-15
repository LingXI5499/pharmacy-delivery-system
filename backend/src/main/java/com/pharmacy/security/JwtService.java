package com.pharmacy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private static final String DENY_PREFIX = "security:jwt:deny:";

    private final SecretKey key;
    private final Duration ttl;
    private final StringRedisTemplate redisTemplate;

    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                      @Value("${app.security.access-token-ttl}") Duration ttl,
                      StringRedisTemplate redisTemplate) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET 至少需要 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
        this.redisTemplate = redisTemplate;
    }

    public String create(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.id()))
                .claim("username", user.username())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isDenied(Claims claims) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(DENY_PREFIX + claims.getId()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public void deny(String token) {
        try {
            Claims claims = parse(token);
            Duration remaining = Duration.between(Instant.now(), claims.getExpiration().toInstant());
            if (!remaining.isNegative() && !remaining.isZero()) {
                redisTemplate.opsForValue().set(DENY_PREFIX + claims.getId(), "1", remaining);
            }
        } catch (RuntimeException ignored) {
            // Logout must still revoke the refresh token when Redis is temporarily unavailable.
        }
    }

    public long expiresInSeconds() {
        return ttl.toSeconds();
    }
}
