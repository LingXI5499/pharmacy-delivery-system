
package com.pharmacy.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class StatusRequest { @NotNull(message = "状态不能为空") @Min(0) @Max(1) private Integer status; }
