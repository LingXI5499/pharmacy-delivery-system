
package com.pharmacy.vo;
import com.pharmacy.enums.UserRole;
public record UserVO(Long id, String username, String nickname, String phone, UserRole role, Integer status) { }
