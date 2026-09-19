
package com.pharmacy.vo;
import java.math.BigDecimal;
public record CartItemVO(Long cartItemId, Long medicineId, String medicineName, String imageUrl, BigDecimal price, Integer stock, Integer quantity, Boolean selected, BigDecimal subtotalAmount, Boolean available, Boolean prescriptionRequired) { }
