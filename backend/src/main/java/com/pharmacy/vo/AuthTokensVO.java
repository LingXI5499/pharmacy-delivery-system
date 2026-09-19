package com.pharmacy.vo;

public record AuthTokensVO(String accessToken, long expiresIn, UserVO user, String redirectPath, String refreshToken) {
    public AuthTokensVO withoutRefreshToken() {
        return new AuthTokensVO(accessToken, expiresIn, user, redirectPath, null);
    }
}
