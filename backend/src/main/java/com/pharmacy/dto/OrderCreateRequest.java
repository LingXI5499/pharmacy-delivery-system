
package com.pharmacy.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class OrderCreateRequest {
    @NotNull(message = "收货地址不能为空") private Long addressId;
    @NotEmpty(message = "至少选择一个购物车商品") private List<Long> cartItemIds;
    private Long prescriptionId;
    @Size(max = 255, message = "备注不能超过 255 个字符") private String userRemark;
}
