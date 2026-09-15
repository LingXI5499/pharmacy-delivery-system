
package com.pharmacy.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class StockAdjustRequest {
    @NotNull(message = "批次不能为空") private Long batchId;
    @NotBlank(message = "调整类型不能为空") private String adjustType;
    @NotNull(message = "调整数量不能为空") @Min(value = 0, message = "数量不能小于 0") private Integer quantity;
    @NotBlank(message = "调整原因不能为空") @Size(max = 255, message = "备注不能超过 255 个字符") private String remark;
}
