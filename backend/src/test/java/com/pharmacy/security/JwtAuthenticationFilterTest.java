package com.pharmacy.security;

import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock private JwtService jwtService;
    @Mock private SysUserMapper userMapper;
    @InjectMocks private JwtAuthenticationFilter filter;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingBearerLeavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void validBearerLoadsRoleAuthority() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("5");
        when(jwtService.parse("good")).thenReturn(claims);
        when(jwtService.isDenied(claims)).thenReturn(false);
        SysUser user = new SysUser();
        user.setId(5L);
        user.setUsername("ph");
        user.setNickname("药师");
        user.setRole(UserRole.PHARMACIST);
        user.setStatus(1);
        when(userMapper.selectById(5L)).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer good");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ROLE_PHARMACIST",
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void deniedTokenDoesNotAuthenticate() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtService.parse("bad")).thenReturn(claims);
        when(jwtService.isDenied(claims)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");
        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
