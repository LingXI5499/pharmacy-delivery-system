package com.pharmacy.audit;

import com.pharmacy.observability.RequestLogContext;
import com.pharmacy.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Trace-Id";

    private final AuditRecorder audit;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String traceId = supplied != null && supplied.matches("[A-Za-z0-9_-]{8,64}")
                ? supplied
                : UUID.randomUUID().toString();
        RequestLogContext.setTraceId(traceId);
        RequestLogContext.setBusinessId(request.getMethod() + " " + request.getRequestURI());
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            if (!"GET".equals(request.getMethod()) && !"OPTIONS".equals(request.getMethod())) {
                try {
                    Object principal = request.getAttribute(AuthenticatedUser.class.getName());
                    AuthenticatedUser user = principal instanceof AuthenticatedUser authenticated ? authenticated : null;
                    audit.record(
                            traceId,
                            user == null ? null : user.id(),
                            user == null ? null : user.role().name(),
                            request.getMethod(),
                            request.getRequestURI(),
                            response.getStatus(),
                            request.getRemoteAddr());
                } catch (RuntimeException e) {
                    log.error("audit persistence failed", e);
                }
            }
            RequestLogContext.clear();
        }
    }
}
