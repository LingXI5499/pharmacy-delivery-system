
package com.pharmacy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String categoryName;
    @Size(max = 255, message = "分类图片地址不能超过 255 个字符")
    private String categoryImage;
    @Size(max = 255, message = "分类描述不能超过 255 个字符")
    private String description;
    @NotNull(message = "排序号不能为空")
    @Min(value = 0, message = "排序号不能小于 0")
    private Integer sortNo;
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0 或 1") @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}
