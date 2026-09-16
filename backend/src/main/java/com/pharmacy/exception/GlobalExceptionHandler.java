package com.pharmacy.exception;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.observability.PharmacyBusinessMetrics;
import com.pharmacy.observability.RequestLogContext;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final PharmacyBusinessMetrics metrics;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        RequestLogContext.setErrorCode(e.getCode());
        metrics.recordErrorCode(e.getCode());
        log.warn("business exception message={}", safeMessage(e.getMessage()));
        HttpStatus status = switch (e.getCode()) {
            case ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.FORBIDDEN -> HttpStatus.FORBIDDEN;
            case ErrorCode.NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCode.STOCK_OR_STATUS_CONFLICT, ErrorCode.ORDER_STATUS_CONFLICT,
                 ErrorCode.CATEGORY_NOT_EMPTY, ErrorCode.ADDRESS_NOT_OWNED, ErrorCode.USERNAME_EXISTS -> HttpStatus.CONFLICT;
            default -> e.getCode() >= 50000 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBinding(ServletRequestBindingException e) {
        RequestLogContext.setErrorCode(ErrorCode.PARAM_INVALID);
        metrics.recordErrorCode(ErrorCode.PARAM_INVALID);
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, "缺少必要的请求参数或请求头");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        RequestLogContext.setErrorCode(ErrorCode.PARAM_INVALID);
        metrics.recordErrorCode(ErrorCode.PARAM_INVALID);
        FieldError error = e.getBindingResult().getFieldError();
        String message = error == null ? "请求参数不合法" : error.getField() + "：" + error.getDefaultMessage();
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException e) {
        RequestLogContext.setErrorCode(ErrorCode.PARAM_INVALID);
        metrics.recordErrorCode(ErrorCode.PARAM_INVALID);
        return ApiResponse.fail(ErrorCode.PARAM_INVALID, e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDuplicate(DuplicateKeyException e) {
        RequestLogContext.setErrorCode(ErrorCode.USERNAME_EXISTS);
        metrics.recordErrorCode(ErrorCode.USERNAME_EXISTS);
        return ApiResponse.fail(ErrorCode.USERNAME_EXISTS, "数据已存在，请检查唯一字段");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleOther(Exception e) {
        RequestLogContext.setErrorCode(ErrorCode.SYSTEM_ERROR);
        metrics.recordErrorCode(ErrorCode.SYSTEM_ERROR);
        log.error("unhandled exception", e);
        return ApiResponse.fail(ErrorCode.SYSTEM_ERROR, "系统内部异常，请稍后重试");
    }

    private static String safeMessage(String message) {
        return RequestLogContext.sanitize(message);
    }
}
