
package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pharmacy_order_item")
public class PharmacyOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long medicineId;
    private String medicineName;
    private String medicineImage;
    private BigDecimal medicinePrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private LocalDateTime createTime;
}
