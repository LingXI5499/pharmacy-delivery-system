package com.pharmacy.controller;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.exception.GlobalExceptionHandler;
import com.pharmacy.security.AuthenticatedUser;
import com.pharmacy.service.AuthService;
import com.pharmacy.vo.AuthTokensVO;
import com.pharmacy.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Duration;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private AuthService service; private MockMvc mvc;
    private final UserVO user=new UserVO(1L,"admin","管理员",null,UserRole.ADMIN,1);
    @BeforeEach void setup(){service=mock(AuthService.class);AuthController c=new AuthController(service);ReflectionTestUtils.setField(c,"refreshTtl",Duration.ofDays(7));ReflectionTestUtils.setField(c,"secureCookie",false);mvc=MockMvcBuilders.standaloneSetup(c).setControllerAdvice(new GlobalExceptionHandler()).build();SecurityContextHolder.clearContext();}
    @Test void register()throws Exception{when(service.register(any())).thenReturn(user);perform(post("/api/auth/register"),"{\"username\":\"user123\",\"password\":\"123456\",\"nickname\":\"测试\"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("admin"));}
    @Test void registerDuplicate()throws Exception{when(service.register(any())).thenThrow(new BusinessException(ErrorCode.USERNAME_EXISTS,"该账号已被注册"));perform(post("/api/auth/register"),"{\"username\":\"user123\",\"password\":\"123456\",\"nickname\":\"测试\"}").andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_EXISTS));}
    @Test void registerValidation()throws Exception{perform(post("/api/auth/register"),"{}").andExpect(status().isBadRequest());}
    @Test void loginSetsHttpOnlyCookie()throws Exception{when(service.login(any(),any(),any())).thenReturn(tokens());perform(post("/api/auth/login"),"{\"username\":\"admin\",\"password\":\"123456\"}").andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("HttpOnly"))).andExpect(jsonPath("$.data.refreshToken").doesNotExist());}
    @Test void loginValidation()throws Exception{perform(post("/api/auth/login"),"{}").andExpect(status().isBadRequest());}
    @Test void refreshRotatesCookie()throws Exception{when(service.refresh(eq("old"),any(),any())).thenReturn(tokens());mvc.perform(post("/api/auth/refresh").cookie(new jakarta.servlet.http.Cookie("refresh_token","old"))).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("new-refresh")));}
    @Test void meUsesPrincipal()throws Exception{authenticate();when(service.me(any())).thenReturn(user);mvc.perform(get("/api/auth/me")).andExpect(jsonPath("$.data.role").value("ADMIN"));}
    @Test void logoutClearsCookie()throws Exception{mvc.perform(post("/api/auth/logout").header("Authorization","Bearer access").cookie(new jakarta.servlet.http.Cookie("refresh_token","refresh"))).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")));verify(service).logout("access","refresh");}
    @Test void loginUsesForwardedClientIp()throws Exception{when(service.login(any(),any(),any())).thenReturn(tokens());perform(post("/api/auth/login").header("X-Forwarded-For","203.0.113.8, 10.0.0.1"),"{\"username\":\"admin\",\"password\":\"123456\"}");verify(service).login(any(),any(),eq("203.0.113.8"));}
    private AuthTokensVO tokens(){return new AuthTokensVO("access",900,user,"/admin/dashboard","new-refresh");}
    private void authenticate(){AuthenticatedUser p=new AuthenticatedUser(1L,"admin","管理员",UserRole.ADMIN);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,List.of()));}
    private org.springframework.test.web.servlet.ResultActions perform(MockHttpServletRequestBuilder r,String body)throws Exception{return mvc.perform(r.contentType(MediaType.APPLICATION_JSON).content(body));}
}
