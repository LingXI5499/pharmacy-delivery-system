package com.pharmacy.procurement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data @TableName("purchase_receipt_item")
public class PurchaseReceiptItem {
    @TableId(type=IdType.AUTO) private Long id;
    private Long receiptId; private Long purchaseOrderItemId; private Long batchId;
    private Integer qualifiedQty; private Integer rejectedQty;
}
