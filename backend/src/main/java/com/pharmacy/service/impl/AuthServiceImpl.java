package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.entity.RefreshToken;
import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.RefreshTokenMapper;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.security.AuthenticatedUser;
import com.pharmacy.security.JwtService;
import com.pharmacy.service.AuthService;
import com.pharmacy.vo.AuthTokensVO;
import com.pharmacy.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final SysUserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.refresh-token-ttl}")
    private Duration refreshTtl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) throw new BusinessException(ErrorCode.USERNAME_EXISTS, "该账号已被注册");
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole(UserRole.USER);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        userMapper.addRole(user.getId(), UserRole.USER.name());
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthTokensVO login(LoginRequest request, String userAgent, String ipAddress) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return issue(user, UUID.randomUUID().toString(), userAgent, ipAddress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BusinessException.class)
    public AuthTokensVO refresh(String rawToken, String userAgent, String ipAddress) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌缺失");
        }
        String oldHash = hash(rawToken);
        RefreshToken stored = refreshTokenMapper.selectForUpdate(oldHash);
        if (stored == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌无效");
        LocalDateTime now = LocalDateTime.now();
        if (stored.getRevokedAt() != null) {
            // BusinessException must not roll back: family revoke has to persist after replay detection.
            refreshTokenMapper.revokeFamily(stored.getFamilyId(), now);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "检测到刷新令牌重放，当前登录已撤销");
        }
        if (!stored.getExpiresAt().isAfter(now)) {
            stored.setRevokedAt(now);
            refreshTokenMapper.updateById(stored);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "刷新令牌已过期");
        }
        SysUser user = userMapper.selectById(stored.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            refreshTokenMapper.revokeFamily(stored.getFamilyId(), now);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不可用");
        }
        AuthTokensVO next = issue(user, stored.getFamilyId(), userAgent, ipAddress);
        stored.setRevokedAt(now);
        stored.setReplacedByHash(hash(next.refreshToken()));
        refreshTokenMapper.updateById(stored);
        return next;
    }

    @Override
    public UserVO me(AuthenticatedUser currentUser) {
        SysUser user = userMapper.selectById(currentUser.id());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不可用");
        }
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) jwtService.deny(accessToken);
        if (refreshToken == null || refreshToken.isBlank()) return;
        RefreshToken stored = refreshTokenMapper.selectForUpdate(hash(refreshToken));
        if (stored != null && stored.getRevokedAt() == null) {
            stored.setRevokedAt(LocalDateTime.now());
            refreshTokenMapper.updateById(stored);
        }
    }

    private AuthTokensVO issue(SysUser user, String familyId, String userAgent, String ipAddress) {
        String rawRefresh = randomToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawRefresh));
        token.setFamilyId(familyId);
        token.setExpiresAt(LocalDateTime.now().plus(refreshTtl));
        token.setUserAgent(limit(userAgent, 255));
        token.setIpAddress(limit(ipAddress, 64));
        token.setCreateTime(LocalDateTime.now());
        refreshTokenMapper.insert(token);
        UserVO vo = toVO(user);
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
        String redirect = switch (user.getRole()) {
            case ADMIN -> "/admin/dashboard";
            case PHARMACIST -> "/pharmacist";
            case PURCHASER -> "/purchaser";
            case WAREHOUSE -> "/warehouse";
            default -> "/home";
        };
        return new AuthTokensVO(jwtService.create(principal), jwtService.expiresInSeconds(), vo, redirect, rawRefresh);
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static UserVO toVO(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(), user.getRole(), user.getStatus());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
