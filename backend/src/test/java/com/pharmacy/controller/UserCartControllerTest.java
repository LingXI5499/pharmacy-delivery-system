package com.pharmacy.controller;

import com.pharmacy.PharmacyDeliveryApplication;
import com.pharmacy.dto.CartAddRequest;
import com.pharmacy.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PharmacyDeliveryApplication.class)
@AutoConfigureMockMvc
class UserCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        session = new MockHttpSession();
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user01");
        loginRequest.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .session(session));
    }

    @Test
    @DisplayName("获取购物车 - 成功")
    void getCart_shouldReturnCart() throws Exception {
        mockMvc.perform(get("/api/user/cart")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("添加商品到购物车 - 成功")
    void addToCart_shouldSucceed() throws Exception {
        CartAddRequest request = new CartAddRequest();
        request.setMedicineId(1L);
        request.setQuantity(2);

        mockMvc.perform(post("/api/user/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("加入购物车成功"));
    }

    @Test
    @DisplayName("获取购物车 - 未登录返回401")
    void getCart_shouldReturn401_whenNotLoggedIn() throws Exception {
        MockHttpSession emptySession = new MockHttpSession();
        mockMvc.perform(get("/api/user/cart")
                        .session(emptySession))
                .andExpect(status().isUnauthorized());
    }
}