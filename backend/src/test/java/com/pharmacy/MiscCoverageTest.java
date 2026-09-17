package com.pharmacy;

import com.pharmacy.audit.AuditRecorder;
import com.pharmacy.audit.TraceIdFilter;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.observability.OutstandingEventMetrics;
import com.pharmacy.security.AuthenticatedUser;
import com.pharmacy.security.CurrentUser;
import com.pharmacy.util.OrderNoUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiscCoverageTest {
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserRequiresAuthenticatedPrincipal() {
        assertEquals(ErrorCode.UNAUTHORIZED,
                assertThrows(BusinessException.class, CurrentUser::id).getCode());
        AuthenticatedUser user = new AuthenticatedUser(7L, "u", "U", com.pharmacy.enums.UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        assertEquals(7L, CurrentUser.id());
    }

    @Test
    void orderNoHasPharmacyPrefix() {
        assertTrue(OrderNoUtil.next().startsWith("PD"));
    }

    @Test
    void outstandingEventMetricsTolerateMissingTable() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("no table"));
        new OutstandingEventMetrics(new SimpleMeterRegistry(), jdbc).refresh();
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(2L);
        new OutstandingEventMetrics(new SimpleMeterRegistry(), jdbc).refresh();
    }

    @Test
    void traceIdFilterAcceptsHeaderAndRecordsMutatingAudit() throws Exception {
        AuditRecorder audit = mock(AuditRecorder.class);
        TraceIdFilter filter = new TraceIdFilter(audit);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/user/orders");
        request.addHeader(TraceIdFilter.HEADER, "trace-id-123456");
        AuthenticatedUser user = new AuthenticatedUser(7L, "u", "U", com.pharmacy.enums.UserRole.USER);
        request.setAttribute(AuthenticatedUser.class.getName(), user);
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(audit).record(anyString(), any(), anyString(), anyString(), anyString(), anyInt(), any());

        doThrow(new RuntimeException("audit down")).when(audit)
                .record(anyString(), any(), anyString(), anyString(), anyString(), anyInt(), any());
        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }
}
