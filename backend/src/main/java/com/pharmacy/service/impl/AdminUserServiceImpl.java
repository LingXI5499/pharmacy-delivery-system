
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.common.PageData;
import com.pharmacy.entity.SysUser;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.SysUserMapper;
import com.pharmacy.service.AdminUserService;
import com.pharmacy.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private final SysUserMapper userMapper;
    @Override public PageData<UserVO> page(long page,long size,String keyword,String role,Integer status){Page<SysUser> p=new Page<>(Math.max(page,1),Math.min(Math.max(size,1),100));LambdaQueryWrapper<SysUser> q=new LambdaQueryWrapper<SysUser>().and(keyword!=null&&!keyword.isBlank(),w->w.like(SysUser::getUsername,keyword).or().like(SysUser::getNickname,keyword).or().like(SysUser::getPhone,keyword)).eq(status!=null,SysUser::getStatus,status).orderByDesc(SysUser::getCreateTime);if(role!=null&&!role.isBlank()){try{q.eq(SysUser::getRole,UserRole.valueOf(role));}catch(IllegalArgumentException e){throw new BusinessException(ErrorCode.PARAM_INVALID,"角色参数不合法");}}userMapper.selectPage(p,q);return PageData.from(p,u->new UserVO(u.getId(),u.getUsername(),u.getNickname(),u.getPhone(),u.getRole(),u.getStatus()));}
    @Override public void updateStatus(Long adminId,Long userId,Integer status){if(adminId.equals(userId))throw new BusinessException(ErrorCode.PARAM_INVALID,"不允许管理员禁用自己");SysUser user=userMapper.selectById(userId);if(user==null)throw new BusinessException(ErrorCode.NOT_FOUND,"用户不存在");user.setStatus(status);user.setUpdateTime(LocalDateTime.now());userMapper.updateById(user);}
}
