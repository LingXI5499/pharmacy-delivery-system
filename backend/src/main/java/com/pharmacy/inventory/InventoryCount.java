package com.pharmacy.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inventory_count")
public class InventoryCount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String countNo;
    private String status;
    private String remark;
    private Long createdBy;
    private Long completedBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completedTime;
}
