package com.pharmacy.observability;

import org.slf4j.MDC;

/**
 * Request-scoped MDC helpers for structured logs.
 * Never put tokens, passwords, or prescription file contents into MDC.
 */
public final class RequestLogContext {
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String BUSINESS_ID = "businessId";
    public static final String ERROR_CODE = "errorCode";

    private RequestLogContext() {
    }

    public static void setTraceId(String traceId) {
        put(TRACE_ID, traceId);
    }

    public static void setUserId(Long userId) {
        put(USER_ID, userId == null ? null : String.valueOf(userId));
    }

    public static void setBusinessId(String businessId) {
        put(BUSINESS_ID, sanitize(businessId));
    }

    public static void setErrorCode(int errorCode) {
        put(ERROR_CODE, String.valueOf(errorCode));
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(USER_ID);
        MDC.remove(BUSINESS_ID);
        MDC.remove(ERROR_CODE);
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase();
        if (lower.contains("bearer ")
                || lower.contains("refresh")
                || lower.contains("password")
                || lower.contains("authorization")
                || (lower.contains("prescription") && lower.contains("/"))) {
            return "[redacted]";
        }
        return trimmed.length() <= 128 ? trimmed : trimmed.substring(0, 128);
    }

    private static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
