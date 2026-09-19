package com.pharmacy.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory_reservation")
public class InventoryReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reservationNo;
    private Long orderId;
    private Long orderItemId;
    private Long batchId;
    private Integer quantity;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
