package com.pharmacy.security;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthenticatedUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已失效");
        }
        return user;
    }

    public static Long id() {
        return require().id();
    }
}
