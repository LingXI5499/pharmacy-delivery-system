package com.pharmacy.service;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.security.AuthenticatedUser;
import com.pharmacy.vo.AuthTokensVO;
import com.pharmacy.vo.UserVO;

public interface AuthService {
    UserVO register(RegisterRequest request);
    AuthTokensVO login(LoginRequest request, String userAgent, String ipAddress);
    AuthTokensVO refresh(String refreshToken, String userAgent, String ipAddress);
    UserVO me(AuthenticatedUser currentUser);
    void logout(String accessToken, String refreshToken);
}
