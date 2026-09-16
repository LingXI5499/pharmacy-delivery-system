package com.pharmacy.procurement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseOrderDetail(
        Long id,
        String purchaseNo,
        Long supplierId,
        String supplierName,
        String status,
        Long applicantId,
        Long approverId,
        LocalDateTime approvedTime,
        String remark,
        String rejectReason,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<PurchaseOrderItemView> items
) {
    public record PurchaseOrderItemView(
            Long id,
            Long medicineId,
            String medicineName,
            Integer orderedQty,
            Integer receivedQty,
            Integer unreceivedQty,
            BigDecimal purchasePrice
    ) {
    }
}
