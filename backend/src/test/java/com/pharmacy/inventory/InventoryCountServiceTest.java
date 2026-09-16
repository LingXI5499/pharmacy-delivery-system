package com.pharmacy.inventory;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryCountServiceTest {
    @Mock private InventoryCountMapper countMapper;
    @Mock private InventoryCountItemMapper itemMapper;
    @Mock private MedicineBatchMapper batchMapper;
    @Mock private InventoryService inventoryService;
    @InjectMocks private InventoryCountService service;

    @Test
    void completeAppliesStockCountOncePerItem() {
        InventoryCount count = counting(9L, "IC-1");
        InventoryCountItem item = item(9L, 5L, 1L, 10, 8);
        when(countMapper.lockById(9L)).thenReturn(count);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(batchMapper.lockById(5L)).thenReturn(batch(5L, 1L, 10));
        when(countMapper.transitionComplete(eq(9L), eq("COUNTING"), eq("COMPLETED"), eq(7L), any(LocalDateTime.class)))
                .thenReturn(1);

        service.complete(9L, 7L);

        verify(inventoryService).applyStockCount(5L, -2, "IC-1", "短缺", 7L);
        verify(countMapper).transitionComplete(eq(9L), eq("COUNTING"), eq("COMPLETED"), eq(7L), any(LocalDateTime.class));
    }

    @Test
    void completeRejectsWhenAlreadyCompleted() {
        InventoryCount count = counting(9L, "IC-1");
        count.setStatus("COMPLETED");
        when(countMapper.lockById(9L)).thenReturn(count);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.complete(9L, 7L));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
        verify(inventoryService, never()).applyStockCount(anyLong(), anyInt(), anyString(), anyString(), anyLong());
    }

    @Test
    void completeFailsWhenTransitionLosesRace() {
        InventoryCount count = counting(9L, "IC-2");
        InventoryCountItem item = item(9L, 5L, 1L, 10, 10);
        when(countMapper.lockById(9L)).thenReturn(count);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(batchMapper.lockById(5L)).thenReturn(batch(5L, 1L, 10));
        when(countMapper.transitionComplete(eq(9L), eq("COUNTING"), eq("COMPLETED"), eq(7L), any(LocalDateTime.class)))
                .thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.complete(9L, 7L));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void upsertItemSnapshotsBookQty() {
        InventoryCount count = counting(3L, "IC-3");
        count.setStatus("DRAFT");
        when(countMapper.lockById(3L)).thenReturn(count);
        when(batchMapper.lockById(5L)).thenReturn(batch(5L, 2L, 12));
        when(itemMapper.selectOne(any())).thenReturn(null);

        service.upsertItem(3L, 5L, 15, "盘盈", 8L);

        ArgumentCaptor<InventoryCountItem> captor = ArgumentCaptor.forClass(InventoryCountItem.class);
        verify(itemMapper).insert(captor.capture());
        assertEquals(12, captor.getValue().getBookQty());
        assertEquals(15, captor.getValue().getCountedQty());
        assertEquals(3, captor.getValue().getDiffQty());
    }

    private static InventoryCount counting(Long id, String no) {
        InventoryCount count = new InventoryCount();
        count.setId(id);
        count.setCountNo(no);
        count.setStatus("COUNTING");
        count.setCreatedBy(1L);
        return count;
    }

    private static InventoryCountItem item(Long countId, Long batchId, Long medicineId, int book, int counted) {
        InventoryCountItem item = new InventoryCountItem();
        item.setId(1L);
        item.setCountId(countId);
        item.setBatchId(batchId);
        item.setMedicineId(medicineId);
        item.setBookQty(book);
        item.setCountedQty(counted);
        item.setDiffQty(counted - book);
        item.setReason("短缺");
        return item;
    }

    private static MedicineBatch batch(Long id, Long medicineId, int available) {
        MedicineBatch batch = new MedicineBatch();
        batch.setId(id);
        batch.setMedicineId(medicineId);
        batch.setAvailableQty(available);
        batch.setReservedQty(0);
        batch.setExpiryDate(LocalDate.now().plusMonths(6));
        batch.setQualityStatus("QUALIFIED");
        batch.setSellable(1);
        return batch;
    }
}
