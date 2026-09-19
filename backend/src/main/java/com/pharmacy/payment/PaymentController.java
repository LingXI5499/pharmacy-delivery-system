package com.pharmacy.payment;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;

    @PostMapping("/api/user/orders/{orderId}/payments")
    public ApiResponse<PaymentAttempt> create(@PathVariable Long orderId) {
        return ApiResponse.success(service.createAttempt(CurrentUser.id(), orderId));
    }

    @PostMapping("/api/user/mock-payments/callback")
    public ApiResponse<Void> callback(@Valid @RequestBody Callback request) {
        service.callback(CurrentUser.id(), request.paymentNo(), request.callbackKey(), request.success(), request.amount());
        return ApiResponse.success(null);
    }

    public record Callback(
            @NotBlank String paymentNo,
            @NotBlank String callbackKey,
            boolean success,
            @NotNull BigDecimal amount) {
    }
}
