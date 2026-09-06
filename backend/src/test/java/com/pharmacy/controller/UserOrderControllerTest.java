package com.pharmacy.controller;

import com.pharmacy.PharmacyDeliveryApplication;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.OrderCreateRequest;
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

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PharmacyDeliveryApplication.class)
@AutoConfigureMockMvc
class UserOrderControllerTest {

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
    @DisplayName("获取订单列表 - 成功")
    void getOrders_shouldReturnPage() throws Exception {
        mockMvc.perform(get("/api/user/orders")
                        .param("page", "1")
                        .param("size", "10")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    @DisplayName("创建订单 - 成功")
    void createOrder_shouldSucceed() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setAddressId(1L);
        request.setCartItemIds(Arrays.asList(1L));
        request.setUserRemark("尽快送达");

        mockMvc.perform(post("/api/user/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("订单提交成功"))
                .andExpect(jsonPath("$.data.orderId").isNumber());
    }

    @Test
    @DisplayName("获取订单列表 - 未登录返回401")
    void getOrders_shouldReturn401_whenNotLoggedIn() throws Exception {
        MockHttpSession emptySession = new MockHttpSession();
        mockMvc.perform(get("/api/user/orders")
                        .session(emptySession))
                .andExpect(status().isUnauthorized());
    }
}