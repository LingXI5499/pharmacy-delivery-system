package com.pharmacy.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchAdjustRequest(
        @NotBlank(message = "调整类型不能为空") String adjustType,
        @NotNull(message = "调整数量不能为空") @Min(value = 0, message = "数量不能小于 0") Integer quantity,
        @NotBlank(message = "调整原因不能为空") @Size(max = 255, message = "原因不能超过 255 个字符") String reason) {
}
