package com.pharmacy.procurement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.Medicine;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.MedicineBatch;
import com.pharmacy.mapper.MedicineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProcurementService {
    private static final String REJECT_PREFIX = "[REJECTED] ";

    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final PurchaseReceiptMapper receiptMapper;
    private final PurchaseReceiptItemMapper receiptItemMapper;
    private final InventoryService inventoryService;
    private final MedicineMapper medicineMapper;

    public List<Supplier> suppliers() {
        return supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().orderByDesc(Supplier::getCreateTime));
    }

    @Transactional
    public Supplier createSupplier(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(request.supplierCode());
        supplier.setSupplierName(request.supplierName());
        supplier.setContactName(request.contactName());
        supplier.setPhone(request.phone());
        supplier.setStatus(1);
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());
        supplierMapper.insert(supplier);
        return supplier;
    }

    @Transactional
    public PurchaseOrder create(Long userId, PurchaseCreateRequest request) {
        Supplier supplier = supplierMapper.selectById(request.supplierId());
        if (supplier == null || !Integer.valueOf(1).equals(supplier.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "供应商不存在或已停用");
        }
        PurchaseOrder order = new PurchaseOrder();
        order.setPurchaseNo("PO" + System.currentTimeMillis());
        order.setSupplierId(request.supplierId());
        order.setStatus("DRAFT");
        order.setApplicantId(userId);
        order.setRemark(request.remark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        for (PurchaseItemRequest itemRequest : request.items()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrderId(order.getId());
            item.setMedicineId(itemRequest.medicineId());
            item.setOrderedQty(itemRequest.quantity());
            item.setReceivedQty(0);
            item.setPurchasePrice(itemRequest.purchasePrice());
            itemMapper.insert(item);
        }
        return order;
    }

    public List<PurchaseOrderDetail> list(String status) {
        LambdaQueryWrapper<PurchaseOrder> query = new LambdaQueryWrapper<PurchaseOrder>()
                .orderByDesc(PurchaseOrder::getCreateTime);
        if (status != null && !status.isBlank()) {
            query.eq(PurchaseOrder::getStatus, status.trim());
        }
        return orderMapper.selectList(query).stream().map(this::toSummary).toList();
    }

    public List<PurchaseOrderDetail> listReceivable() {
        return orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                        .in(PurchaseOrder::getStatus, List.of("APPROVED", "PARTIALLY_RECEIVED"))
                        .orderByDesc(PurchaseOrder::getUpdateTime))
                .stream()
                .map(this::toDetail)
                .toList();
    }

    public PurchaseOrderDetail detail(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
        }
        return toDetail(order);
    }

    @Transactional
    public void approve(Long adminId, Long id) {
        PurchaseOrder order = orderMapper.lockById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "只有草稿采购单可以审批");
        }
        orderMapper.update(null, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, id)
                .eq(PurchaseOrder::getStatus, "DRAFT")
                .set(PurchaseOrder::getStatus, "APPROVED")
                .set(PurchaseOrder::getApproverId, adminId)
                .set(PurchaseOrder::getApprovedTime, LocalDateTime.now())
                .set(PurchaseOrder::getUpdateTime, LocalDateTime.now()));
    }

    @Transactional
    public void reject(Long adminId, Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "拒绝原因不能为空");
        }
        PurchaseOrder order = orderMapper.lockById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "只有草稿采购单可以拒绝");
        }
        String rejectRemark = REJECT_PREFIX + reason.trim();
        if (order.getRemark() != null && !order.getRemark().isBlank()) {
            rejectRemark = order.getRemark() + " | " + rejectRemark;
        }
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, id)
                .eq(PurchaseOrder::getStatus, "DRAFT")
                .set(PurchaseOrder::getStatus, "REJECTED")
                .set(PurchaseOrder::getApproverId, adminId)
                .set(PurchaseOrder::getApprovedTime, LocalDateTime.now())
                .set(PurchaseOrder::getRemark, rejectRemark)
                .set(PurchaseOrder::getUpdateTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "采购单审批状态已变化");
        }
    }

    @Transactional
    public PurchaseReceipt receive(Long warehouseUserId, ReceiptRequest request) {
        PurchaseOrder order = orderMapper.lockById(request.purchaseOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "采购单不存在");
        }
        if (!List.of("APPROVED", "PARTIALLY_RECEIVED").contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT, "采购单当前不可收货");
        }
        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.setReceiptNo("PR" + System.currentTimeMillis());
        receipt.setPurchaseOrderId(order.getId());
        receipt.setReceiverId(warehouseUserId);
        receipt.setReceivedTime(LocalDateTime.now());
        receipt.setRemark(request.remark());
        receipt.setCreateTime(LocalDateTime.now());
        receiptMapper.insert(receipt);
        for (ReceiptItemRequest itemRequest : request.items()) {
            PurchaseOrderItem item = itemMapper.selectById(itemRequest.purchaseOrderItemId());
            if (item == null || !item.getPurchaseOrderId().equals(order.getId())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "收货明细不属于当前采购单");
            }
            int total = itemRequest.qualifiedQty() + itemRequest.rejectedQty();
            if (itemMapper.addReceived(item.getId(), total) != 1) {
                throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "收货数量超过未收数量");
            }
            MedicineBatch batch = inventoryService.receive(
                    item.getMedicineId(),
                    itemRequest.batchNo(),
                    itemRequest.productionDate(),
                    itemRequest.expiryDate(),
                    item.getPurchasePrice(),
                    itemRequest.qualifiedQty(),
                    receipt.getReceiptNo(),
                    warehouseUserId);
            PurchaseReceiptItem receiptItem = new PurchaseReceiptItem();
            receiptItem.setReceiptId(receipt.getId());
            receiptItem.setPurchaseOrderItemId(item.getId());
            receiptItem.setBatchId(batch.getId());
            receiptItem.setQualifiedQty(itemRequest.qualifiedQty());
            receiptItem.setRejectedQty(itemRequest.rejectedQty());
            receiptItemMapper.insert(receiptItem);
        }
        long remaining = itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getPurchaseOrderId, order.getId()))
                .stream()
                .filter(item -> item.getReceivedQty() < item.getOrderedQty())
                .count();
        orderMapper.update(null, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getId, order.getId())
                .set(PurchaseOrder::getStatus, remaining == 0 ? "RECEIVED" : "PARTIALLY_RECEIVED")
                .set(PurchaseOrder::getUpdateTime, LocalDateTime.now()));
        return receipt;
    }

    private PurchaseOrderDetail toSummary(PurchaseOrder order) {
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        return new PurchaseOrderDetail(
                order.getId(),
                order.getPurchaseNo(),
                order.getSupplierId(),
                supplier == null ? null : supplier.getSupplierName(),
                order.getStatus(),
                order.getApplicantId(),
                order.getApproverId(),
                order.getApprovedTime(),
                order.getRemark(),
                extractRejectReason(order),
                order.getCreateTime(),
                order.getUpdateTime(),
                List.of());
    }

    private PurchaseOrderDetail toDetail(PurchaseOrder order) {
        Supplier supplier = supplierMapper.selectById(order.getSupplierId());
        List<PurchaseOrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getPurchaseOrderId, order.getId())
                .orderByAsc(PurchaseOrderItem::getId));
        List<PurchaseOrderDetail.PurchaseOrderItemView> views = new ArrayList<>();
        for (PurchaseOrderItem item : items) {
            Medicine medicine = medicineMapper.selectById(item.getMedicineId());
            int ordered = Objects.requireNonNullElse(item.getOrderedQty(), 0);
            int received = Objects.requireNonNullElse(item.getReceivedQty(), 0);
            views.add(new PurchaseOrderDetail.PurchaseOrderItemView(
                    item.getId(),
                    item.getMedicineId(),
                    medicine == null ? ("药品#" + item.getMedicineId()) : medicine.getMedicineName(),
                    ordered,
                    received,
                    Math.max(ordered - received, 0),
                    item.getPurchasePrice()));
        }
        return new PurchaseOrderDetail(
                order.getId(),
                order.getPurchaseNo(),
                order.getSupplierId(),
                supplier == null ? null : supplier.getSupplierName(),
                order.getStatus(),
                order.getApplicantId(),
                order.getApproverId(),
                order.getApprovedTime(),
                order.getRemark(),
                extractRejectReason(order),
                order.getCreateTime(),
                order.getUpdateTime(),
                views);
    }

    private static String extractRejectReason(PurchaseOrder order) {
        if (order.getRemark() == null) {
            return null;
        }
        int index = order.getRemark().indexOf(REJECT_PREFIX);
        if (index < 0) {
            return "REJECTED".equals(order.getStatus()) ? order.getRemark() : null;
        }
        return order.getRemark().substring(index + REJECT_PREFIX.length()).trim();
    }
}
