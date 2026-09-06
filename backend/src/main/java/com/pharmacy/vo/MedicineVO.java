
package com.pharmacy.vo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record MedicineVO(Long id, Long categoryId, String categoryName, String medicineName, String imageUrl, String description, String usageInstruction, String precautions, BigDecimal price, Integer stock, Integer warningStock, Integer status, Boolean isLowStock, LocalDateTime createTime, LocalDateTime updateTime) { }
