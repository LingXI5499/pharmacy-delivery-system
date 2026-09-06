package com.pharmacy.service;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.vo.UserVO;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
public interface AuthService { UserVO register(RegisterRequest request); Map<String,Object> login(LoginRequest request, HttpSession session); UserVO me(HttpSession session); void logout(HttpSession session); }
