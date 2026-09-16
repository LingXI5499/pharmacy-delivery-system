package com.pharmacy.service;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.entity.RefreshToken;
import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.RefreshTokenMapper;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.security.JwtService;
import com.pharmacy.service.impl.AuthServiceImpl;
import com.pharmacy.vo.AuthTokensVO;
import com.pharmacy.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private SysUserMapper userMapper;
    @Mock private RefreshTokenMapper refreshTokenMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setRefreshTtl() {
        ReflectionTestUtils.setField(authService, "refreshTtl", Duration.ofDays(7));
    }

    @Test
    void shouldRegisterNormalUserWhenUsernameIsAvailable() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user_new");
        request.setPassword("123456");
        request.setNickname("新用户");
        request.setPhone("13900000000");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUser.class).setId(10L);
            return 1;
        });
        UserVO result = authService.register(request);
        assertEquals(10L, result.id());
        assertEquals("user_new", result.username());
        assertEquals("USER", result.role().name());
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("bcrypt-hash", captor.getValue().getPassword());
    }

    @Test
    void loginIssuesAccessAndRefreshTokens() {
        SysUser user = activeUser(8L, "alice", UserRole.USER);
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("secret", user.getPassword())).thenReturn(true);
        when(jwtService.create(any())).thenReturn("access-token");
        when(jwtService.expiresInSeconds()).thenReturn(900L);
        AuthTokensVO tokens = authService.login(request, "JUnit", "127.0.0.1");
        assertEquals("access-token", tokens.accessToken());
        assertNotNull(tokens.refreshToken());
        verify(refreshTokenMapper).insert(any(RefreshToken.class));
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(captor.capture());
        assertEquals(hash(tokens.refreshToken()), captor.getValue().getTokenHash());
        assertEquals(8L, captor.getValue().getUserId());
    }

    @Test
    void refreshRotatesTokenAndMarksPreviousReplaced() {
        String oldRaw = "old-refresh-token-value";
        RefreshToken stored = storedToken(oldRaw, "family-1", null);
        SysUser user = activeUser(8L, "alice", UserRole.USER);
        when(refreshTokenMapper.selectForUpdate(hash(oldRaw))).thenReturn(stored);
        when(userMapper.selectById(8L)).thenReturn(user);
        when(jwtService.create(any())).thenReturn("next-access");
        when(jwtService.expiresInSeconds()).thenReturn(900L);

        AuthTokensVO next = authService.refresh(oldRaw, "JUnit", "127.0.0.1");

        assertEquals("next-access", next.accessToken());
        assertNotNull(next.refreshToken());
        verify(refreshTokenMapper).insert(any(RefreshToken.class));
        ArgumentCaptor<RefreshToken> updated = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).updateById(updated.capture());
        assertNotNull(updated.getValue().getRevokedAt());
        assertEquals(hash(next.refreshToken()), updated.getValue().getReplacedByHash());
        assertEquals("family-1", updated.getValue().getFamilyId());
    }

    @Test
    void refreshReplayRevokesEntireTokenFamily() {
        String oldRaw = "replayed-refresh";
        RefreshToken stored = storedToken(oldRaw, "family-replay", LocalDateTime.now().minusMinutes(1));
        when(refreshTokenMapper.selectForUpdate(hash(oldRaw))).thenReturn(stored);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refresh(oldRaw, "JUnit", "127.0.0.1"));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        verify(refreshTokenMapper).revokeFamily(eq("family-replay"), any(LocalDateTime.class));
        verify(refreshTokenMapper, never()).insert(any(RefreshToken.class));
        verify(jwtService, never()).create(any());
    }

    @Test
    void refreshRejectsExpiredToken() {
        String oldRaw = "expired-refresh";
        RefreshToken stored = storedToken(oldRaw, "family-exp", null);
        stored.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(refreshTokenMapper.selectForUpdate(hash(oldRaw))).thenReturn(stored);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refresh(oldRaw, "JUnit", "127.0.0.1"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
        verify(refreshTokenMapper).updateById(stored);
        verify(refreshTokenMapper, never()).revokeFamily(any(), any());
    }

    @Test
    void logoutDeniesAccessTokenAndRevokesRefresh() {
        String refreshRaw = "logout-refresh";
        RefreshToken stored = storedToken(refreshRaw, "family-out", null);
        when(refreshTokenMapper.selectForUpdate(hash(refreshRaw))).thenReturn(stored);

        authService.logout("access-to-deny", refreshRaw);

        verify(jwtService).deny("access-to-deny");
        assertNotNull(stored.getRevokedAt());
        verify(refreshTokenMapper).updateById(stored);
    }

    @Test
    void logoutStillDeniesAccessWhenRefreshMissing() {
        authService.logout("access-only", null);
        verify(jwtService).deny("access-only");
        verify(refreshTokenMapper, never()).selectForUpdate(any());
    }

    private static SysUser activeUser(Long id, String username, UserRole role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        user.setPassword("hash");
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    private static RefreshToken storedToken(String raw, String familyId, LocalDateTime revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setUserId(8L);
        token.setTokenHash(hash(raw));
        token.setFamilyId(familyId);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        token.setRevokedAt(revokedAt);
        token.setCreateTime(LocalDateTime.now().minusHours(1));
        return token;
    }

    private static String hash(String raw) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
