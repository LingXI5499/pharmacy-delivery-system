
package com.pharmacy.util;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import jakarta.servlet.http.HttpSession;

public final class SessionUtil {
    public static final String LOGIN_USER = "LOGIN_USER";
    private SessionUtil() { }

    public static SessionUser current(HttpSession session) {
        Object user = session == null ? null : session.getAttribute(LOGIN_USER);
        if (user instanceof SessionUser sessionUser) return sessionUser;
        return null;
    }

    public static SessionUser require(HttpSession session) {
        SessionUser user = current(session);
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已失效");
        return user;
    }

    public static SessionUser currentAdmin(HttpSession session) {
        SessionUser user = require(session);
        if (user.role() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无管理员权限");
        }
        return user;
    }
}
