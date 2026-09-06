
package com.pharmacy.common;

import java.time.LocalDateTime;

public record ApiResponse<T>(Integer code, String message, T data, LocalDateTime timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }
}
