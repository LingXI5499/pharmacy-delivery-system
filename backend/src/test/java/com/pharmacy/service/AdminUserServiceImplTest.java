package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {
    @Mock private SysUserMapper userMapper;
    @InjectMocks private AdminUserServiceImpl adminUserService;

    @Test
    void pageRejectsIllegalRole() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.page(1, 10, "a", "NOT_A_ROLE", 1));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void pageFiltersKeywordAndRole() {
        when(userMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<SysUser> page = invocation.getArgument(0);
            SysUser user = new SysUser();
            user.setId(2L);
            user.setUsername("alice");
            user.setRole(UserRole.USER);
            user.setStatus(1);
            page.setRecords(List.of(user));
            page.setTotal(1);
            return page;
        });
        assertEquals(1, adminUserService.page(1, 10, "ali", "USER", 1).total());
    }

    @Test
    void cannotDisableSelf() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateStatus(1L, 1L, 0));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void updateStatusRequiresExistingUser() {
        when(userMapper.selectById(9L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateStatus(1L, 9L, 0));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void cannotRemoveOwnAdminRole() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateRole(1L, 1L, "USER"));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void updateRoleWritesUserRoleTable() {
        SysUser user = new SysUser();
        user.setId(9L);
        when(userMapper.lockById(9L)).thenReturn(user);
        when(userMapper.addRole(9L, "PHARMACIST")).thenReturn(1);
        adminUserService.updateRole(1L, 9L, "PHARMACIST");
        verify(userMapper).clearRoles(9L);
        verify(userMapper).addRole(9L, "PHARMACIST");
    }

    @Test
    void updateRoleFailsWhenRoleRowMissing() {
        SysUser user = new SysUser();
        user.setId(9L);
        when(userMapper.lockById(9L)).thenReturn(user);
        when(userMapper.addRole(9L, "WAREHOUSE")).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminUserService.updateRole(1L, 9L, "WAREHOUSE"));
        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getCode());
    }
}
