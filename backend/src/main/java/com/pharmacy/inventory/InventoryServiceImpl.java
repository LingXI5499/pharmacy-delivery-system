package com.pharmacy.inventory;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final MedicineBatchMapper batchMapper;
    private final InventoryReservationMapper reservationMapper;
    private final InventoryLedgerMapper ledgerMapper;
    private final MedicineMapper medicineMapper;

    @Override
    @Transactional
    public boolean canReserve(List<PharmacyOrderItem> items) {
        for (PharmacyOrderItem item : items) {
            int available = batchMapper.selectSellableForUpdate(item.getMedicineId(), LocalDate.now())
                    .stream().mapToInt(MedicineBatch::getAvailableQty).sum();
            if (available < item.getQuantity()) return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames={"catalogPages","catalogMedicine"},allEntries=true)
    public void reserve(Long orderId, List<PharmacyOrderItem> items, LocalDateTime expiresAt, Long operatorId) {
        for (PharmacyOrderItem item : items) {
            int remaining = item.getQuantity();
            List<MedicineBatch> batches = batchMapper.selectSellableForUpdate(item.getMedicineId(), LocalDate.now());
            for (MedicineBatch batch : batches) {
                if (remaining == 0) break;
                int allocated = Math.min(remaining, batch.getAvailableQty());
                if (allocated == 0) continue;
                if (batchMapper.reserve(batch.getId(), allocated) != 1) {
                    throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "库存发生并发变化，请重试");
                }
                if (medicineMapper.decreaseStock(batch.getMedicineId(), allocated) != 1) {
                    throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "药品聚合库存发生并发变化，请重试");
                }
                InventoryReservation reservation = new InventoryReservation();
                reservation.setReservationNo(UUID.randomUUID().toString());
                reservation.setOrderId(orderId);
                reservation.setOrderItemId(item.getId());
                reservation.setBatchId(batch.getId());
                reservation.setQuantity(allocated);
                reservation.setStatus("ACTIVE");
                reservation.setExpiresAt(expiresAt);
                reservation.setCreateTime(LocalDateTime.now());
                reservation.setUpdateTime(LocalDateTime.now());
                reservationMapper.insert(reservation);
                appendLedger("ORDER_RESERVE", orderId.toString(), batch, -allocated, allocated, operatorId, "订单库存预占");
                remaining -= allocated;
            }
            if (remaining > 0) {
                throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "可售批次库存不足：" + item.getMedicineName());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSale(Long orderId, Long operatorId) {
        for (InventoryReservation reservation : reservationMapper.activeForUpdate(orderId)) {
            MedicineBatch batch = batchMapper.selectById(reservation.getBatchId());
            if (batchMapper.confirm(batch.getId(), reservation.getQuantity()) != 1) {
                throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "预占库存确认失败");
            }
            mark(reservation, "COMMITTED");
            appendLedger("SALE_COMMIT", orderId.toString(), batch, 0, -reservation.getQuantity(), operatorId, "支付成功销售出库");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames={"catalogPages","catalogMedicine"},allEntries=true)
    public void release(Long orderId, String reason, Long operatorId) {
        for (InventoryReservation reservation : reservationMapper.activeForUpdate(orderId)) {
            MedicineBatch batch = batchMapper.selectById(reservation.getBatchId());
            if (batchMapper.release(batch.getId(), reservation.getQuantity()) != 1) {
                throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "预占库存释放失败");
            }
            medicineMapper.restoreStock(batch.getMedicineId(), reservation.getQuantity());
            mark(reservation, "RELEASED");
            appendLedger("ORDER_RELEASE", orderId.toString(), batch, reservation.getQuantity(), -reservation.getQuantity(), operatorId, reason);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames={"catalogPages","catalogMedicine"},allEntries=true)
    public MedicineBatch receive(Long medicineId, String batchNo, LocalDate productionDate, LocalDate expiryDate,
                                 BigDecimal purchasePrice, int qualifiedQty, String receiptNo, Long operatorId) {
        Long locationId = batchMapper.locationId("MAIN");
        if (locationId == null) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "主仓库位未初始化");
        MedicineBatch batch=batchMapper.findBatchForUpdate(medicineId,locationId,batchNo);
        if(batch==null){batch=new MedicineBatch();batch.setMedicineId(medicineId);batch.setLocationId(locationId);batch.setBatchNo(batchNo);batch.setProductionDate(productionDate);batch.setExpiryDate(expiryDate);batch.setPurchasePrice(purchasePrice);batch.setAvailableQty(0);batch.setReservedQty(0);batch.setQualityStatus("QUALIFIED");batch.setSellable(1);batch.setVersion(0);batch.setCreateTime(LocalDateTime.now());batch.setUpdateTime(LocalDateTime.now());batchMapper.insert(batch);}
        else if(!productionDate.equals(batch.getProductionDate())||!expiryDate.equals(batch.getExpiryDate()))throw new BusinessException(ErrorCode.PARAM_INVALID,"同一批号的生产日期或失效日期不一致");
        if(qualifiedQty>0){MedicineBatch before=batchMapper.selectById(batch.getId());batchMapper.addAvailable(batch.getId(),qualifiedQty);medicineMapper.restoreStock(medicineId,qualifiedQty);appendLedger("PURCHASE_RECEIPT",receiptNo,before,qualifiedQty,0,operatorId,"采购验收入库");}
        return batch;
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    @CacheEvict(cacheNames={"catalogPages","catalogMedicine"},allEntries=true)
    public void adjustBatch(Long batchId,String adjustType,int quantity,String reason,Long operatorId){
        if(reason==null||reason.isBlank())throw new BusinessException(ErrorCode.PARAM_INVALID,"库存调整必须填写原因");
        MedicineBatch batch=batchMapper.lockById(batchId);if(batch==null)throw new BusinessException(ErrorCode.NOT_FOUND,"库存批次不存在");
        int delta=switch(adjustType==null?"":adjustType.trim().toUpperCase()){case "INCREMENT"->quantity;case "DECREMENT"->-quantity;case "SET"->quantity-batch.getAvailableQty();default->throw new BusinessException(ErrorCode.PARAM_INVALID,"adjustType 仅支持 INCREMENT、DECREMENT、SET");};
        if(batchMapper.adjustAvailable(batchId,delta)!=1)throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT,"调整后可用库存不能小于零");
        if(delta>0)medicineMapper.restoreStock(batch.getMedicineId(),delta);else if(delta<0&&medicineMapper.decreaseStock(batch.getMedicineId(),-delta)!=1)throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT,"药品聚合库存不足");
        appendLedger("STOCK_ADJUST",batchId.toString(),batch,delta,0,operatorId,reason);
    }

    @Override
    @Transactional(rollbackFor=Exception.class)
    @CacheEvict(cacheNames={"catalogPages","catalogMedicine"},allEntries=true)
    public void refundRestock(Long orderId,Long operatorId){for(InventoryReservation reservation:reservationMapper.committedForUpdate(orderId)){MedicineBatch batch=batchMapper.selectById(reservation.getBatchId());batchMapper.addAvailable(batch.getId(),reservation.getQuantity());medicineMapper.restoreStock(batch.getMedicineId(),reservation.getQuantity());mark(reservation,"RESTOCKED");appendLedger("REFUND_RESTOCK",orderId.toString(),batch,reservation.getQuantity(),0,operatorId,"退款完成，未发货库存回补");}}

    private void mark(InventoryReservation reservation, String status) {
        reservationMapper.update(null, new LambdaUpdateWrapper<InventoryReservation>()
                .eq(InventoryReservation::getId, reservation.getId()).eq(InventoryReservation::getStatus, "ACTIVE")
                .set(InventoryReservation::getStatus, status).set(InventoryReservation::getUpdateTime, LocalDateTime.now()));
    }

    private void appendLedger(String type, String businessId, MedicineBatch before,
                              int availableDelta, int reservedDelta, Long operatorId, String reason) {
        InventoryLedger ledger = new InventoryLedger();
        ledger.setEventNo(UUID.randomUUID().toString());
        ledger.setBusinessType(type);
        ledger.setBusinessId(businessId);
        ledger.setMedicineId(before.getMedicineId());
        ledger.setBatchId(before.getId());
        ledger.setAvailableDelta(availableDelta);
        ledger.setReservedDelta(reservedDelta);
        ledger.setAvailableAfter(before.getAvailableQty() + availableDelta);
        ledger.setReservedAfter(before.getReservedQty() + reservedDelta);
        ledger.setOperatorId(operatorId);
        ledger.setReason(reason);
        ledger.setCreateTime(LocalDateTime.now());
        ledgerMapper.insert(ledger);
    }
}
