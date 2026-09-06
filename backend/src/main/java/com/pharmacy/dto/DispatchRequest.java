
package com.pharmacy.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class DispatchRequest { @NotNull(message = "请选择骑手") private Long riderId; @Size(max = 255, message = "备注不能超过 255 个字符") private String adminRemark; }
