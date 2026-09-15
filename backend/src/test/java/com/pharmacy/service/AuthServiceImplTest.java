
package com.pharmacy.service;

import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.entity.SysUser;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.service.impl.AuthServiceImpl;
import com.pharmacy.vo.UserVO;
import com.pharmacy.mapper.RefreshTokenMapper;
import com.pharmacy.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private SysUserMapper userMapper;
    @Mock private RefreshTokenMapper refreshTokenMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void shouldRegisterNormalUserWhenUsernameIsAvailable() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user_new"); request.setPassword("123456"); request.setNickname("新用户"); request.setPhone("13900000000");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("bcrypt-hash");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> { invocation.getArgument(0, SysUser.class).setId(10L); return 1; });
        UserVO result = authService.register(request);
        assertEquals(10L, result.id());
        assertEquals("user_new", result.username());
        assertEquals("USER", result.role().name());
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("bcrypt-hash", captor.getValue().getPassword());
    }
}
