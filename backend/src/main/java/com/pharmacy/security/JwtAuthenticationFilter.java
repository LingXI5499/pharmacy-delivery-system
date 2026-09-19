package com.pharmacy.security;

import com.pharmacy.entity.SysUser;
import com.pharmacy.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.pharmacy.observability.RequestLogContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(header.substring(7));
                if (!jwtService.isDenied(claims)) {
                    SysUser entity = userMapper.selectById(Long.valueOf(claims.getSubject()));
                    if (entity != null && Integer.valueOf(1).equals(entity.getStatus())) {
                        AuthenticatedUser user = new AuthenticatedUser(entity.getId(), entity.getUsername(), entity.getNickname(), entity.getRole());
                        request.setAttribute(AuthenticatedUser.class.getName(), user);
                        RequestLogContext.setUserId(user.id());
                        var authority = new SimpleGrantedAuthority("ROLE_" + entity.getRole().name());
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, List.of(authority)));
                    }
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
