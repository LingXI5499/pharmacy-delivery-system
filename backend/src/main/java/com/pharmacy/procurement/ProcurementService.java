package com.pharmacy.procurement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.MedicineBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class ProcurementService {
    private final SupplierMapper supplierMapper; private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper; private final PurchaseReceiptMapper receiptMapper;
    private final PurchaseReceiptItemMapper receiptItemMapper; private final InventoryService inventoryService;

    public List<Supplier> suppliers(){return supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().orderByDesc(Supplier::getCreateTime));}
    @Transactional public Supplier createSupplier(SupplierRequest r){Supplier s=new Supplier();s.setSupplierCode(r.supplierCode());s.setSupplierName(r.supplierName());s.setContactName(r.contactName());s.setPhone(r.phone());s.setStatus(1);s.setCreateTime(LocalDateTime.now());s.setUpdateTime(LocalDateTime.now());supplierMapper.insert(s);return s;}

    @Transactional public PurchaseOrder create(Long userId,PurchaseCreateRequest r){
        Supplier supplier=supplierMapper.selectById(r.supplierId());
        if(supplier==null||!Integer.valueOf(1).equals(supplier.getStatus()))throw new BusinessException(ErrorCode.PARAM_INVALID,"供应商不存在或已停用");
        PurchaseOrder order=new PurchaseOrder();order.setPurchaseNo("PO"+System.currentTimeMillis());order.setSupplierId(r.supplierId());order.setStatus("DRAFT");order.setApplicantId(userId);order.setRemark(r.remark());order.setCreateTime(LocalDateTime.now());order.setUpdateTime(LocalDateTime.now());orderMapper.insert(order);
        for(PurchaseItemRequest x:r.items()){PurchaseOrderItem item=new PurchaseOrderItem();item.setPurchaseOrderId(order.getId());item.setMedicineId(x.medicineId());item.setOrderedQty(x.quantity());item.setReceivedQty(0);item.setPurchasePrice(x.purchasePrice());itemMapper.insert(item);}return order;
    }
    @Transactional public void approve(Long adminId,Long id){PurchaseOrder order=orderMapper.lockById(id);if(order==null)throw new BusinessException(ErrorCode.NOT_FOUND,"采购单不存在");if(!"DRAFT".equals(order.getStatus()))throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"只有草稿采购单可以审批");orderMapper.update(null,new LambdaUpdateWrapper<PurchaseOrder>().eq(PurchaseOrder::getId,id).eq(PurchaseOrder::getStatus,"DRAFT").set(PurchaseOrder::getStatus,"APPROVED").set(PurchaseOrder::getApproverId,adminId).set(PurchaseOrder::getApprovedTime,LocalDateTime.now()));}

    @Transactional public PurchaseReceipt receive(Long warehouseUserId,ReceiptRequest r){
        PurchaseOrder order=orderMapper.lockById(r.purchaseOrderId());
        if(order==null)throw new BusinessException(ErrorCode.NOT_FOUND,"采购单不存在");
        if(!List.of("APPROVED","PARTIALLY_RECEIVED").contains(order.getStatus()))throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"采购单当前不可收货");
        PurchaseReceipt receipt=new PurchaseReceipt();receipt.setReceiptNo("PR"+System.currentTimeMillis());receipt.setPurchaseOrderId(order.getId());receipt.setReceiverId(warehouseUserId);receipt.setReceivedTime(LocalDateTime.now());receipt.setRemark(r.remark());receipt.setCreateTime(LocalDateTime.now());receiptMapper.insert(receipt);
        for(ReceiptItemRequest x:r.items()){
            PurchaseOrderItem item=itemMapper.selectById(x.purchaseOrderItemId());
            if(item==null||!item.getPurchaseOrderId().equals(order.getId()))throw new BusinessException(ErrorCode.PARAM_INVALID,"收货明细不属于当前采购单");
            int total=x.qualifiedQty()+x.rejectedQty();if(itemMapper.addReceived(item.getId(),total)!=1)throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT,"收货数量超过未收数量");
            MedicineBatch batch=inventoryService.receive(item.getMedicineId(),x.batchNo(),x.productionDate(),x.expiryDate(),item.getPurchasePrice(),x.qualifiedQty(),receipt.getReceiptNo(),warehouseUserId);
            PurchaseReceiptItem ri=new PurchaseReceiptItem();ri.setReceiptId(receipt.getId());ri.setPurchaseOrderItemId(item.getId());ri.setBatchId(batch.getId());ri.setQualifiedQty(x.qualifiedQty());ri.setRejectedQty(x.rejectedQty());receiptItemMapper.insert(ri);
        }
        long remaining=itemMapper.selectList(new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getPurchaseOrderId,order.getId())).stream().filter(i->i.getReceivedQty()<i.getOrderedQty()).count();
        orderMapper.update(null,new LambdaUpdateWrapper<PurchaseOrder>().eq(PurchaseOrder::getId,order.getId()).set(PurchaseOrder::getStatus,remaining==0?"RECEIVED":"PARTIALLY_RECEIVED"));return receipt;
    }
}
