package com.pharmacy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmacy.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAndPermissionTest {
    @Test
    void permissionLookupFailureDoesNotGrantAccess() {
        SysUserMapper users = mock(SysUserMapper.class);
        PermissionService service = new PermissionService(users);
        when(users.countPermission(any(), anyString())).thenThrow(new RuntimeException("db down"));
        assertFalse(service.has("inventory.read"));
    }

    @Test
    void permissionTrueWhenCountPositive() {
        SysUserMapper users = mock(SysUserMapper.class);
        PermissionService service = new PermissionService(users);
        AuthenticatedUser principal = new AuthenticatedUser(8L, "alice", "Alice", com.pharmacy.enums.UserRole.USER);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
        when(users.countPermission(8L, "inventory.read")).thenReturn(1L);
        assertTrue(service.has("inventory.read"));
        when(users.countPermission(8L, "inventory.write")).thenReturn(0L);
        assertFalse(service.has("inventory.write"));
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void loginLimitReturns429AfterThreshold() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(21L);
        ObjectMapper json = new ObjectMapper();
        json.registerModule(new JavaTimeModule());
        RateLimitFilter filter = new RateLimitFilter(redis, json);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("10.0.0.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void redisFailureDoesNotBlockOrderPost() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));
        RateLimitFilter filter = new RateLimitFilter(redis, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/user/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void getRequestsAreNotRateLimited() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RateLimitFilter filter = new RateLimitFilter(redis, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(redis, never()).opsForValue();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void firstHitSetsExpiry() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);
        RateLimitFilter filter = new RateLimitFilter(redis, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(redis).expire(anyString(), any(Duration.class));
        verify(chain).doFilter(any(), any());
    }
}
