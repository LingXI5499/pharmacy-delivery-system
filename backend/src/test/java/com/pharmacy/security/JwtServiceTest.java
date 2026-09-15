package com.pharmacy.security;

import com.pharmacy.enums.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    private static final String SECRET = "local-development-secret-change-before-deploy-2026";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, Duration.ofMinutes(15), redisTemplate);
    }

    @Test
    void createAndParseRoundTrip() {
        AuthenticatedUser user = new AuthenticatedUser(7L, "alice", "爱丽丝", UserRole.USER);
        String token = jwtService.create(user);
        var claims = jwtService.parse(token);
        assertEquals("7", claims.getSubject());
        assertEquals("alice", claims.get("username", String.class));
        assertEquals("USER", claims.get("role", String.class));
        assertEquals(900, jwtService.expiresInSeconds());
    }

    @Test
    void expiredTokenCannotBeParsed() throws InterruptedException {
        JwtService shortLived = new JwtService(SECRET, Duration.ofMillis(40), redisTemplate);
        String token = shortLived.create(new AuthenticatedUser(1L, "u", "n", UserRole.USER));
        Thread.sleep(80);
        assertThrows(ExpiredJwtException.class, () -> shortLived.parse(token));
    }

    @Test
    void denyMarksJtiInRedisAndIsDeniedReadsIt() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        AuthenticatedUser user = new AuthenticatedUser(3L, "bob", "鲍勃", UserRole.PHARMACIST);
        String token = jwtService.create(user);
        jwtService.deny(token);
        verify(valueOperations).set(startsWith("security:jwt:deny:"), eq("1"), any(Duration.class));
        assertTrue(jwtService.isDenied(jwtService.parse(token)));
    }

    @Test
    void isDeniedReturnsFalseWhenRedisUnavailable() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("redis down"));
        var claims = jwtService.parse(jwtService.create(new AuthenticatedUser(1L, "u", "n", UserRole.USER)));
        assertFalse(jwtService.isDenied(claims));
    }

    @Test
    void denySwallowsRedisFailureWithoutThrowing() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        String token = jwtService.create(new AuthenticatedUser(1L, "u", "n", UserRole.USER));
        jwtService.deny(token);
        verify(redisTemplate, never()).hasKey(anyString());
    }
}
