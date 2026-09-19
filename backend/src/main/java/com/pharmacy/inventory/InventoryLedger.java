package com.pharmacy.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory_ledger")
public class InventoryLedger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventNo;
    private String businessType;
    private String businessId;
    private Long medicineId;
    private Long batchId;
    private Integer availableDelta;
    private Integer reservedDelta;
    private Integer availableAfter;
    private Integer reservedAfter;
    private Long operatorId;
    private String reason;
    private LocalDateTime createTime;
}
