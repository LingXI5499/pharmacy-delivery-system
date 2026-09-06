package com.pharmacy.controller;

import com.pharmacy.PharmacyDeliveryApplication;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.util.SessionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PharmacyDeliveryApplication.class)
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysUserMapper userMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    @Test
    @DisplayName("注册 - 成功注册新用户")
    void register_shouldSucceed_whenValidRequest() throws Exception {
        String username = "test_register_" + System.currentTimeMillis();
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("123456");
        request.setNickname("测试用户");
        request.setPhone("13900000000");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("注册成功"))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        SysUser user = userMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        assertNotNull(user);
        assertEquals("123456", user.getPassword());
    }

    @Test
    @DisplayName("注册 - 失败当用户名已存在")
    void register_shouldFail_whenUsernameExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin");
        request.setPassword("123456");
        request.setNickname("测试");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40002))
                .andExpect(jsonPath("$.message").value("该账号已被注册"));
    }

    @Test
    @DisplayName("注册 - 失败当缺少必填字段")
    void register_shouldFail_whenMissingRequiredFields() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登录 - 成功登录")
    void login_shouldSucceed_whenValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.redirectPath").value("/admin/dashboard"))
                .andReturn();

        assertNotNull(session.getAttribute(SessionUtil.LOGIN_USER));
    }

    @Test
    @DisplayName("登录 - 失败当密码错误")
    void login_shouldFail_whenWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003))
                .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    @Test
    @DisplayName("登录 - 失败当用户不存在")
    void login_shouldFail_whenUserNotExists() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40003))
                .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    @Test
    @DisplayName("获取用户信息 - 成功当已登录")
    void me_shouldReturnUser_whenLoggedIn() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .session(session));

        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    @DisplayName("获取用户信息 - 返回null当未登录")
    void me_shouldReturnNull_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("登出 - 成功")
    void logout_shouldSucceed() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .session(session));

        assertNotNull(session.getAttribute(SessionUtil.LOGIN_USER));

        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertTrue(session.isInvalid());
    }
}