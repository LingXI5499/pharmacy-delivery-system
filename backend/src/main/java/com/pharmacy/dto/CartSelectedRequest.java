
package com.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class CartSelectedRequest { @NotNull(message = "勾选状态不能为空") private Boolean selected; }
