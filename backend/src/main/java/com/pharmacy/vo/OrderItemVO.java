
package com.pharmacy.vo;
import java.math.BigDecimal;
public record OrderItemVO(Long id, Long medicineId, String medicineName, String medicineImage, BigDecimal medicinePrice, Integer quantity, BigDecimal subtotalAmount) { }
