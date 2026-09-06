
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.dto.LoginRequest;
import com.pharmacy.dto.RegisterRequest;
import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.service.AuthService;
import com.pharmacy.util.SessionUser;
import com.pharmacy.util.SessionUtil;
import com.pharmacy.vo.UserVO;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final SysUserMapper userMapper;

    @Override
    public UserVO register(RegisterRequest request) {
        long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (count > 0) throw new BusinessException(ErrorCode.USERNAME_EXISTS, "该账号已被注册");
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword()); // 课程演示：按需求明文保存，真实系统禁止如此实现
        user.setNickname(request.getNickname());
        user.setPhone(blankToNull(request.getPhone()));
        user.setRole(UserRole.USER);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public Map<String, Object> login(LoginRequest request, HttpSession session) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "账号或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        UserVO vo = toVO(user);
        session.setAttribute(SessionUtil.LOGIN_USER, new SessionUser(vo.id(), vo.username(), vo.nickname(), vo.role()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", vo);
        result.put("redirectPath", vo.role() == UserRole.ADMIN ? "/admin/dashboard" : "/home");
        return result;
    }

    @Override
    public UserVO me(HttpSession session) {
        SessionUser current = SessionUtil.current(session);
        if (current == null) return null;
        SysUser user = userMapper.selectById(current.id());
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            session.invalidate();
            return null;
        }
        return toVO(user);
    }

    @Override
    public void logout(HttpSession session) {
        if (session != null) session.invalidate();
    }

    private static UserVO toVO(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getPhone(), user.getRole(), user.getStatus());
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
}
