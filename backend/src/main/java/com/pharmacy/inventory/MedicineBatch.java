package com.pharmacy.inventory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medicine_batch")
public class MedicineBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long medicineId;
    private Long locationId;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal purchasePrice;
    private Integer availableQty;
    private Integer reservedQty;
    private String qualityStatus;
    private Integer sellable;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
