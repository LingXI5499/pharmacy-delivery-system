package com.pharmacy.controller;

import com.pharmacy.PharmacyDeliveryApplication;
import com.pharmacy.dto.AddressRequest;
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
class UserAddressControllerTest {

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
    @DisplayName("获取地址列表 - 成功")
    void listAddresses_shouldReturnList() throws Exception {
        mockMvc.perform(get("/api/user/addresses")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("添加地址 - 成功")
    void addAddress_shouldSucceed() throws Exception {
        AddressRequest request = new AddressRequest();
        request.setReceiverName("测试收货人");
        request.setReceiverPhone("13900000001");
        request.setProvince("北京市");
        request.setCity("北京市");
        request.setDistrict("朝阳区");
        request.setDetailAddress("测试地址123号");

        mockMvc.perform(post("/api/user/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("新增地址成功"))
                .andExpect(jsonPath("$.data.receiverName").value("测试收货人"));
    }

    @Test
    @DisplayName("获取地址列表 - 未登录返回401")
    void listAddresses_shouldReturn401_whenNotLoggedIn() throws Exception {
        MockHttpSession emptySession = new MockHttpSession();
        mockMvc.perform(get("/api/user/addresses")
                        .session(emptySession))
                .andExpect(status().isUnauthorized());
    }
}