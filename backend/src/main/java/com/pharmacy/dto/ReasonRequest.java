
package com.pharmacy.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class ReasonRequest { @NotBlank(message = "取消原因不能为空") @Size(max = 255, message = "取消原因不能超过 255 个字符") private String reason; }
