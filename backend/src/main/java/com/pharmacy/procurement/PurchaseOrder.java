package com.pharmacy.procurement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type=IdType.AUTO) private Long id;
    private String purchaseNo; private Long supplierId; private String status; private Long applicantId;
    private Long approverId; private LocalDateTime approvedTime; private String remark;
    private LocalDateTime createTime; private LocalDateTime updateTime;
}
