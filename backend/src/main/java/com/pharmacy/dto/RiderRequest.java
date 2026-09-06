
package com.pharmacy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class RiderRequest {
    @NotBlank(message = "骑手姓名不能为空") @Size(max = 32, message = "骑手姓名不能超过 32 个字符") private String riderName;
    @NotBlank(message = "骑手手机号不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") private String phone;
    @NotNull(message = "状态不能为空") @Min(0) @Max(1) private Integer status;
    @Size(max = 255, message = "备注不能超过 255 个字符") private String remark;
}
