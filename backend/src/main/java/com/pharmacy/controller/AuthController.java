package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.security.CurrentUser;
import com.pharmacy.service.AuthService;
import com.pharmacy.vo.AuthTokensVO;
import com.pharmacy.vo.UserVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh_token";
    private final AuthService authService;

    @Value("${app.security.refresh-token-ttl}") private Duration refreshTtl;
    @Value("${app.security.refresh-cookie-secure}") private boolean secureCookie;

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokensVO> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest servletRequest,
                                           HttpServletResponse response) {
        AuthTokensVO tokens = authService.login(request, servletRequest.getHeader("User-Agent"), clientIp(servletRequest));
        setRefreshCookie(response, tokens.refreshToken(), refreshTtl);
        return ApiResponse.success("登录成功", tokens.withoutRefreshToken());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokensVO> refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthTokensVO tokens = authService.refresh(cookie(request, REFRESH_COOKIE), request.getHeader("User-Agent"), clientIp(request));
        setRefreshCookie(response, tokens.refreshToken(), refreshTtl);
        return ApiResponse.success(tokens.withoutRefreshToken());
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.success(authService.me(CurrentUser.require()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(bearer(request), cookie(request, REFRESH_COOKIE));
        setRefreshCookie(response, "", Duration.ZERO);
        return ApiResponse.success("退出登录成功", null);
    }

    private void setRefreshCookie(HttpServletResponse response, String value, Duration age) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true).secure(secureCookie).sameSite("Lax")
                .path("/api/auth").maxAge(age).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies).filter(c -> name.equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);
    }

    private static String bearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
