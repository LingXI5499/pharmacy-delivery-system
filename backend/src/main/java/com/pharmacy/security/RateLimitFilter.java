package com.pharmacy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component @RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis; private final ObjectMapper json;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        int limit=limit(request);if(limit>0){try{String ip=request.getRemoteAddr();String key="rate:"+request.getRequestURI()+":"+ip+":"+Instant.now().getEpochSecond()/60;Long count=redis.opsForValue().increment(key);if(count!=null&&count==1)redis.expire(key,Duration.ofMinutes(2));if(count!=null&&count>limit){response.setStatus(429);response.setContentType(MediaType.APPLICATION_JSON_VALUE);response.setCharacterEncoding("UTF-8");json.writeValue(response.getWriter(),ApiResponse.fail(ErrorCode.RATE_LIMITED,"请求过于频繁，请稍后再试"));return;}}catch(RuntimeException ignored){/* Redis failure must not break core correctness. */}}
        chain.doFilter(request,response);
    }
    private int limit(HttpServletRequest r){if(!"POST".equals(r.getMethod()))return 0;if("/api/auth/login".equals(r.getRequestURI()))return 20;if("/api/user/orders".equals(r.getRequestURI()))return 30;return 0;}
}
