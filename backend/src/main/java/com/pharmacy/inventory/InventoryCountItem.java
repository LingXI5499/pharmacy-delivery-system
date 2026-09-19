package com.pharmacy.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory_count_item")
public class InventoryCountItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long countId;
    private Long batchId;
    private Long medicineId;
    private Integer bookQty;
    private Integer countedQty;
    private Integer diffQty;
    private String reason;
    private Long operatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
