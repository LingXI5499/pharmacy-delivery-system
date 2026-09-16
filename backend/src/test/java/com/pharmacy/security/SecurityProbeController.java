package com.pharmacy.security;

import com.pharmacy.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebMvcTest-only stub. Profile keeps it off full {@code @SpringBootTest} scans
 * so it does not collide with production {@code /api/auth/me} and pharmacist routes.
 */
@Profile("security-probe")
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
