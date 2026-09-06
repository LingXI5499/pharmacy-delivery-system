
package com.pharmacy.interceptor;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.enums.UserRole;
import com.pharmacy.util.SessionUser;
import com.pharmacy.util.SessionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper;
    public AuthInterceptor(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/") || uri.startsWith("/api/public/")) return true;
        Object value = request.getSession(false) == null ? null : request.getSession(false).getAttribute(SessionUtil.LOGIN_USER);
        if (!(value instanceof SessionUser user)) {
            writeError(response, ErrorCode.UNAUTHORIZED, "未登录或登录已失效");
            return false;
        }
        if (uri.startsWith("/api/admin/") && user.role() != UserRole.ADMIN) {
            writeError(response, ErrorCode.FORBIDDEN, "无管理员权限");
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code == ErrorCode.UNAUTHORIZED ? 401 : 403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}
