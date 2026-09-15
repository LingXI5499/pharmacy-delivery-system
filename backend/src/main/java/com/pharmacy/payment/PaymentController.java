package com.pharmacy.payment;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController @RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    @PostMapping("/api/user/orders/{orderId}/payments") public ApiResponse<PaymentAttempt> create(@PathVariable Long orderId){return ApiResponse.success(service.createAttempt(CurrentUser.id(),orderId));}
    @PostMapping("/api/user/mock-payments/callback") public ApiResponse<Void> callback(@Valid @RequestBody Callback r){service.callback(CurrentUser.id(),r.paymentNo(),r.callbackKey(),r.success());return ApiResponse.success(null);}
    public record Callback(@NotBlank String paymentNo,@NotBlank String callbackKey,boolean success){}
}
