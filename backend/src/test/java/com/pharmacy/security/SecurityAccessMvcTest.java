package com.pharmacy.security;

import com.pharmacy.audit.AuditRecorder;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.config.SecurityConfig;
import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("security-probe")
class SecurityAccessMvcTest {
    @Autowired private MockMvc mvc;
    @MockBean private JwtService jwtService;
    @MockBean private SysUserMapper userMapper;
    @MockBean private AuditRecorder auditRecorder;
    @MockBean private StringRedisTemplate stringRedisTemplate;

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void insufficientRoleReturns403() throws Exception {
        stubBearer("token-user", 21L, UserRole.USER);
        mvc.perform(get("/api/pharmacist/prescriptions").header("Authorization", "Bearer token-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN));
    }

    @Test
    void pharmacistRoleCanAccessPharmacistPath() throws Exception {
        stubBearer("token-rx", 22L, UserRole.PHARMACIST);
        mvc.perform(get("/api/pharmacist/prescriptions").header("Authorization", "Bearer token-rx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("pharmacist-ok"));
    }

    @Test
    void deniedAccessTokenIsTreatedAsUnauthenticated() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("23");
        when(jwtService.parse("denied-token")).thenReturn(claims);
        when(jwtService.isDenied(claims)).thenReturn(true);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer denied-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void expiredOrInvalidTokenReturns401() throws Exception {
        when(jwtService.parse(anyString())).thenThrow(new RuntimeException("expired"));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer stale"))
                .andExpect(status().isUnauthorized());
    }

    private void stubBearer(String rawToken, Long userId, UserRole role) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(String.valueOf(userId));
        when(jwtService.parse(rawToken)).thenReturn(claims);
        when(jwtService.isDenied(claims)).thenReturn(false);
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("u" + userId);
        user.setNickname("n" + userId);
        user.setRole(role);
        user.setStatus(1);
        when(userMapper.selectById(userId)).thenReturn(user);
    }
}
