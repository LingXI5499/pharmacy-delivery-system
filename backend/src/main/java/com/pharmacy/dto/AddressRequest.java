
package com.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank(message = "收货人不能为空") @Size(max = 32, message = "收货人不能超过 32 个字符") private String receiverName;
    @NotBlank(message = "联系电话不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String receiverPhone;
    @Size(max = 32, message = "省份不能超过 32 个字符") private String province;
    @Size(max = 32, message = "城市不能超过 32 个字符") private String city;
    @Size(max = 32, message = "区县不能超过 32 个字符") private String district;
    @NotBlank(message = "详细地址不能为空") @Size(max = 255, message = "详细地址不能超过 255 个字符") private String detailAddress;
    private Boolean isDefault = false;
}
