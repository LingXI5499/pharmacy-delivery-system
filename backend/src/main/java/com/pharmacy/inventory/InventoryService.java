package com.pharmacy.inventory;

import com.pharmacy.entity.PharmacyOrderItem;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    boolean canReserve(List<PharmacyOrderItem> items);
    void reserve(Long orderId, List<PharmacyOrderItem> items, LocalDateTime expiresAt, Long operatorId);
    void confirmSale(Long orderId, Long operatorId);
    void release(Long orderId, String reason, Long operatorId);
    MedicineBatch receive(Long medicineId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
                          BigDecimal purchasePrice, int qualifiedQty, String receiptNo, Long operatorId);
    void adjustBatch(Long batchId, String adjustType, int quantity, String reason, Long operatorId);
    void refundRestock(Long orderId, Long operatorId);
}
