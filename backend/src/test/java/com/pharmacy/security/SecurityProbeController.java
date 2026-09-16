package com.pharmacy.security;

import com.pharmacy.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityProbeController {
    @GetMapping("/api/auth/me")
    ApiResponse<String> me() {
        return ApiResponse.success("me-ok");
    }

    @GetMapping("/api/pharmacist/prescriptions")
    ApiResponse<String> pharmacist() {
        return ApiResponse.success("pharmacist-ok");
    }
}
