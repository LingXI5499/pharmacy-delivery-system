package com.pharmacy.service;
import com.pharmacy.common.PageData;
import com.pharmacy.vo.UserVO;
public interface AdminUserService { PageData<UserVO> page(long page,long size,String keyword,String role,Integer status); void updateStatus(Long adminId,Long userId,Integer status); void updateRole(Long adminId,Long userId,String role); }
