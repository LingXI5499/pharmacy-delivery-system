
package com.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MedicineRequest {
    @NotNull(message = "分类不能为空") private Long categoryId;
    @NotBlank(message = "药品名称不能为空") @Size(max = 100, message = "药品名称不能超过 100 个字符") private String medicineName;
    @Size(max = 255, message = "图片地址不能超过 255 个字符") private String imageUrl;
    @Size(max = 500, message = "简介不能超过 500 个字符") private String description;
    @Size(max = 1000, message = "使用说明不能超过 1000 个字符") private String usageInstruction;
    @Size(max = 1000, message = "注意事项不能超过 1000 个字符") private String precautions;
    @NotNull(message = "价格不能为空") @DecimalMin(value = "0.01", message = "价格必须大于 0") private BigDecimal price;
    @NotNull(message = "库存不能为空") @Min(value = 0, message = "库存不能小于 0") private Integer stock;
    @NotNull(message = "预警库存不能为空") @Min(value = 0, message = "预警库存不能小于 0") private Integer warningStock;
    @NotNull(message = "状态不能为空") @Min(0) @Max(1) private Integer status;
}
