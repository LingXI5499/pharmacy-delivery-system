
package com.pharmacy.controller;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.service.AuthService;
import com.pharmacy.vo.UserVO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register") public ApiResponse<UserVO> register(@Valid @RequestBody RegisterRequest request){return ApiResponse.success("注册成功",authService.register(request));}
    @PostMapping("/login") public ApiResponse<Map<String,Object>> login(@Valid @RequestBody LoginRequest request,HttpSession session){return ApiResponse.success("登录成功",authService.login(request,session));}
    @GetMapping("/me") public ApiResponse<UserVO> me(HttpSession session){return ApiResponse.success(authService.me(session));}
    @PostMapping("/logout") public ApiResponse<Void> logout(HttpSession session){authService.logout(session);return ApiResponse.success("退出登录成功",null);}
}
