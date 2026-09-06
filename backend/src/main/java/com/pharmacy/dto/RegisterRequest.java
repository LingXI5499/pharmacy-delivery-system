
package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "账号为 4~32 位字母、数字或下划线")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为 6~32 位")
    private String password;
    @NotBlank(message = "昵称不能为空")
    @Size(min = 2, max = 32, message = "昵称长度为 2~32 位")
    private String nickname;
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
}
