package com.pharmacy.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryCountService {
    private final InventoryCountMapper countMapper;
    private final InventoryCountItemMapper itemMapper;
    private final MedicineBatchMapper batchMapper;
    private final InventoryService inventoryService;

    @Transactional(rollbackFor = Exception.class)
    public InventoryCount create(Long operatorId, String remark) {
        InventoryCount count = new InventoryCount();
        count.setCountNo("IC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        count.setStatus("DRAFT");
        count.setRemark(remark);
        count.setCreatedBy(operatorId);
        count.setCreateTime(LocalDateTime.now());
        count.setUpdateTime(LocalDateTime.now());
        countMapper.insert(count);
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void startCounting(Long countId) {
        if (countMapper.transitionStatus(countId, "DRAFT", "COUNTING") != 1) {
            throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "只有草稿盘点单可以开始盘点");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryCountItem upsertItem(Long countId, Long batchId, int countedQty, String reason, Long operatorId) {
        if (countedQty < 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "实盘数量不能为负");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "盘点项必须填写原因");
        }
        InventoryCount count = requireEditable(countId);
        MedicineBatch batch = batchMapper.lockById(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "库存批次不存在");
        }
        InventoryCountItem existing = itemMapper.selectOne(new LambdaQueryWrapper<InventoryCountItem>()
                .eq(InventoryCountItem::getCountId, countId)
                .eq(InventoryCountItem::getBatchId, batchId));
        if (existing == null) {
            InventoryCountItem item = new InventoryCountItem();
            item.setCountId(countId);
            item.setBatchId(batchId);
            item.setMedicineId(batch.getMedicineId());
            item.setBookQty(batch.getAvailableQty());
            item.setCountedQty(countedQty);
            item.setDiffQty(countedQty - batch.getAvailableQty());
            item.setReason(reason.trim());
            item.setOperatorId(operatorId);
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            itemMapper.insert(item);
            touch(count);
            return item;
        }
        existing.setBookQty(batch.getAvailableQty());
        existing.setCountedQty(countedQty);
        existing.setDiffQty(countedQty - batch.getAvailableQty());
        existing.setReason(reason.trim());
        existing.setOperatorId(operatorId);
        existing.setUpdateTime(LocalDateTime.now());
        itemMapper.updateById(existing);
        touch(count);
        return existing;
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long countId, Long operatorId) {
        InventoryCount count = countMapper.lockById(countId);
        if (count == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点单不存在");
        }
        if (!"COUNTING".equals(count.getStatus())) {
            throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "只有盘点中的单据可以完成");
        }
        List<InventoryCountItem> items = itemMapper.selectList(new LambdaQueryWrapper<InventoryCountItem>()
                .eq(InventoryCountItem::getCountId, countId)
                .orderByAsc(InventoryCountItem::getBatchId));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "盘点单没有盘点项");
        }
        for (InventoryCountItem item : items) {
            if (item.getCountedQty() == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "存在未录入实盘数量的盘点项");
            }
            if (item.getReason() == null || item.getReason().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "存在未填写原因的盘点项");
            }
            MedicineBatch batch = batchMapper.lockById(item.getBatchId());
            if (batch == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "库存批次不存在");
            }
            int delta = item.getCountedQty() - item.getBookQty();
            item.setDiffQty(delta);
            item.setUpdateTime(LocalDateTime.now());
            itemMapper.updateById(item);
            inventoryService.applyStockCount(item.getBatchId(), delta, count.getCountNo(), item.getReason(), operatorId);
        }
        if (countMapper.transitionComplete(countId, "COUNTING", "COMPLETED", operatorId, LocalDateTime.now()) != 1) {
            throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "盘点单已被其他人完成或状态已变更");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long countId) {
        InventoryCount count = countMapper.lockById(countId);
        if (count == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点单不存在");
        }
        if (!"DRAFT".equals(count.getStatus()) && !"COUNTING".equals(count.getStatus())) {
            throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "已完成或已取消的盘点单不能再取消");
        }
        if (countMapper.transitionStatus(countId, count.getStatus(), "CANCELED") != 1) {
            throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "盘点单状态变更失败");
        }
    }

    public InventoryCount get(Long id) {
        InventoryCount count = countMapper.selectById(id);
        if (count == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点单不存在");
        }
        return count;
    }

    public List<InventoryCount> list(String status) {
        return countMapper.selectList(new LambdaQueryWrapper<InventoryCount>()
                .eq(status != null && !status.isBlank(), InventoryCount::getStatus, status)
                .orderByDesc(InventoryCount::getCreateTime)
                .last("LIMIT 200"));
    }

    public List<InventoryCountItem> items(Long countId) {
        get(countId);
        return itemMapper.selectList(new LambdaQueryWrapper<InventoryCountItem>()
                .eq(InventoryCountItem::getCountId, countId)
                .orderByAsc(InventoryCountItem::getId));
    }

    public Map<String, Object> detail(Long countId) {
        Map<String, Object> result = new HashMap<>();
        result.put("count", get(countId));
        result.put("items", items(countId));
        return result;
    }

    public List<Map<String, Object>> reconciliation() {
        return countMapper.findStockMismatches();
    }

    private InventoryCount requireEditable(Long countId) {
        InventoryCount count = countMapper.lockById(countId);
        if (count == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "盘点单不存在");
        }
        if (!"DRAFT".equals(count.getStatus()) && !"COUNTING".equals(count.getStatus())) {
            throw new BusinessException(ErrorCode.STOCK_OR_STATUS_CONFLICT, "当前盘点单不可编辑");
        }
        return count;
    }

    private void touch(InventoryCount count) {
        count.setUpdateTime(LocalDateTime.now());
        countMapper.updateById(count);
    }
}
