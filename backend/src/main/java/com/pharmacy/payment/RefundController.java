package com.pharmacy.payment;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class RefundController {
    private final RefundService service;

    @PostMapping("/api/user/orders/{orderId}/refunds")
    public ApiResponse<RefundRecord> request(@PathVariable Long orderId, @Valid @RequestBody Reason reason) {
        return ApiResponse.success(service.request(CurrentUser.id(), orderId, reason.reason()));
    }

    @PostMapping("/api/admin/mock-refunds/callback")
    public ApiResponse<Void> callback(@Valid @RequestBody Callback request) {
        service.callback(request.refundNo(), request.callbackKey(), request.success(), request.amount());
        return ApiResponse.success(null);
    }

    public record Reason(@NotBlank @Size(max = 255) String reason) {
    }

    public record Callback(
            @NotBlank String refundNo,
            @NotBlank @Size(max = 80) String callbackKey,
            boolean success,
            @NotNull BigDecimal amount) {
    }
}
