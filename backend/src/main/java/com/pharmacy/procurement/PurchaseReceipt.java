package com.pharmacy.procurement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("purchase_receipt")
public class PurchaseReceipt {
    @TableId(type=IdType.AUTO) private Long id;
    private String receiptNo; private Long purchaseOrderId; private Long receiverId;
    private LocalDateTime receivedTime; private String remark; private LocalDateTime createTime;
}
