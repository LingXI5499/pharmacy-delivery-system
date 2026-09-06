
package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pharmacy.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pharmacy_order")
public class PharmacyOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal productAmount;
    private BigDecimal deliveryFee;
    private BigDecimal orderAmount;
    private OrderStatus orderStatus;
    private Long riderId;
    private String riderName;
    private String riderPhone;
    private String userRemark;
    private String adminRemark;
    private LocalDateTime acceptedTime;
    private LocalDateTime packedTime;
    private LocalDateTime dispatchedTime;
    private LocalDateTime completedTime;
    private LocalDateTime canceledTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
