package com.pharmacy.security;

import com.pharmacy.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("permissionService") @RequiredArgsConstructor
public class PermissionService {
    private final SysUserMapper users;
    public boolean has(String permission){try{return users.countPermission(CurrentUser.id(),permission)>0;}catch(RuntimeException e){return false;}}
}
