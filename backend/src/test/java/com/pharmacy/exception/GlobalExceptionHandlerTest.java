package com.pharmacy.exception;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.observability.PharmacyBusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {
    private final PharmacyBusinessMetrics metrics = new PharmacyBusinessMetrics(new SimpleMeterRegistry());
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(metrics);

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsBusinessCodesToHttpStatus() {
        assertEquals(HttpStatus.UNAUTHORIZED, handler.handleBusiness(new BusinessException(ErrorCode.UNAUTHORIZED, "未登录")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, handler.handleBusiness(new BusinessException(ErrorCode.FORBIDDEN, "禁止")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, handler.handleBusiness(new BusinessException(ErrorCode.NOT_FOUND, "不存在")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, handler.handleBusiness(new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "冲突")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, handler.handleBusiness(new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "冲突")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, handler.handleBusiness(new BusinessException(ErrorCode.ADDRESS_NOT_OWNED, "地址")).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, handler.handleBusiness(new BusinessException(ErrorCode.SYSTEM_ERROR, "内部")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, handler.handleBusiness(new BusinessException(ErrorCode.PARAM_INVALID, "参数")).getStatusCode());
    }

    @Test
    void validationAndDuplicateHandlersStayStable() throws Exception {
        assertEquals(ErrorCode.PARAM_INVALID, handler.handleBinding(new ServletRequestBindingException("missing")).code());
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
        binding.addError(new FieldError("req", "reason", "不能为空"));
        MethodArgumentNotValidException invalid = mock(MethodArgumentNotValidException.class);
        org.mockito.Mockito.when(invalid.getBindingResult()).thenReturn(binding);
        assertTrue(handler.handleValidation(invalid).message().contains("reason"));
        assertEquals(ErrorCode.PARAM_INVALID, handler.handleConstraint(new ConstraintViolationException("bad", Set.of())).code());
        assertEquals(ErrorCode.USERNAME_EXISTS, handler.handleDuplicate(new DuplicateKeyException("dup")).code());
        assertEquals(ErrorCode.SYSTEM_ERROR, handler.handleOther(new IllegalStateException("boom")).code());
    }

    @Test
    void metricsClassifyInventoryAuthAndServerErrors() {
        metrics.recordErrorCode(ErrorCode.STOCK_OR_STATUS_CONFLICT);
        metrics.recordErrorCode(ErrorCode.ORDER_STATUS_CONFLICT);
        metrics.recordErrorCode(ErrorCode.UNAUTHORIZED);
        metrics.recordErrorCode(ErrorCode.SYSTEM_ERROR);
        metrics.recordErrorCode(ErrorCode.PARAM_INVALID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anon", null));
    }
}
