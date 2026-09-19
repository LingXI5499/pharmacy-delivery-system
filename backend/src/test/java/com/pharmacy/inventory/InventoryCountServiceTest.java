package com.pharmacy.inventory;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.support.MybatisPlusLambdaInit;
import org.junit.jupiter.api.BeforeAll;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @BeforeAll
    static void initLambdaCache() {
        MybatisPlusLambdaInit.entities(InventoryCount.class, InventoryCountItem.class);
    }

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

    @Test
    void createStartCancelAndList() {
        when(countMapper.insert(any(InventoryCount.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, InventoryCount.class).setId(1L);
            return 1;
        });
        assertEquals("DRAFT", service.create(8L, "月初盘点").getStatus());
        when(countMapper.transitionStatus(1L, "DRAFT", "COUNTING")).thenReturn(1);
        service.startCounting(1L);
        when(countMapper.transitionStatus(1L, "DRAFT", "COUNTING")).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.startCounting(1L)).getCode());

        InventoryCount draft = counting(2L, "IC-2");
        draft.setStatus("DRAFT");
        when(countMapper.lockById(2L)).thenReturn(draft);
        when(countMapper.transitionStatus(2L, "DRAFT", "CANCELED")).thenReturn(1);
        service.cancel(2L);
        InventoryCount done = counting(3L, "IC-3");
        done.setStatus("COMPLETED");
        when(countMapper.lockById(3L)).thenReturn(done);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.cancel(3L)).getCode());
        when(countMapper.selectById(9L)).thenReturn(counting(9L, "IC-1"));
        when(itemMapper.selectList(any())).thenReturn(List.of());
        service.detail(9L);
        service.list("COUNTING");
        when(countMapper.findStockMismatches()).thenReturn(List.of());
        assertTrue(service.reconciliation().isEmpty());
    }

    @Test
    void upsertRejectsNegativeQtyAndCompletedCount() {
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.upsertItem(1L, 5L, -1, "原因", 8L)).getCode());
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.upsertItem(1L, 5L, 1, " ", 8L)).getCode());
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.upsertItem(1L, 5L, 1, null, 8L)).getCode());
        InventoryCount completed = counting(1L, "IC");
        completed.setStatus("COMPLETED");
        when(countMapper.lockById(1L)).thenReturn(completed);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.upsertItem(1L, 5L, 1, "原因", 8L)).getCode());
        when(countMapper.lockById(2L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.upsertItem(2L, 5L, 1, "原因", 8L)).getCode());
    }

    @Test
    void upsertUpdatesExistingItemAndRejectsMissingBatch() {
        InventoryCount draft = counting(3L, "IC-3");
        draft.setStatus("DRAFT");
        when(countMapper.lockById(3L)).thenReturn(draft);
        when(batchMapper.lockById(5L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.upsertItem(3L, 5L, 9, "盘盈", 8L)).getCode());

        when(batchMapper.lockById(5L)).thenReturn(batch(5L, 2L, 12));
        InventoryCountItem existing = item(3L, 5L, 2L, 12, 8);
        when(itemMapper.selectOne(any())).thenReturn(existing);
        InventoryCountItem updated = service.upsertItem(3L, 5L, 15, "复盘", 8L);
        assertEquals(15, updated.getCountedQty());
        assertEquals(3, updated.getDiffQty());
        verify(itemMapper).updateById(existing);
    }

    @Test
    void completeRejectsMissingCountEmptyItemsAndIncompleteRows() {
        when(countMapper.lockById(9L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.complete(9L, 7L)).getCode());

        when(countMapper.lockById(9L)).thenReturn(counting(9L, "IC-1"));
        when(itemMapper.selectList(any())).thenReturn(List.of());
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.complete(9L, 7L)).getCode());

        InventoryCountItem missingQty = item(9L, 5L, 1L, 10, 8);
        missingQty.setCountedQty(null);
        when(itemMapper.selectList(any())).thenReturn(List.of(missingQty));
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.complete(9L, 7L)).getCode());

        InventoryCountItem blankReason = item(9L, 5L, 1L, 10, 8);
        blankReason.setReason(" ");
        when(itemMapper.selectList(any())).thenReturn(List.of(blankReason));
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.complete(9L, 7L)).getCode());

        InventoryCountItem nullReason = item(9L, 5L, 1L, 10, 8);
        nullReason.setReason(null);
        when(itemMapper.selectList(any())).thenReturn(List.of(nullReason));
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.complete(9L, 7L)).getCode());

        when(itemMapper.selectList(any())).thenReturn(List.of(item(9L, 5L, 1L, 10, 8)));
        when(batchMapper.lockById(5L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.complete(9L, 7L)).getCode());
    }

    @Test
    void cancelCountingAndGetMissing() {
        InventoryCount counting = counting(4L, "IC-4");
        when(countMapper.lockById(4L)).thenReturn(counting);
        when(countMapper.transitionStatus(4L, "COUNTING", "CANCELED")).thenReturn(1);
        service.cancel(4L);
        verify(countMapper).transitionStatus(4L, "COUNTING", "CANCELED");

        when(countMapper.lockById(5L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.cancel(5L)).getCode());

        InventoryCount draft = counting(6L, "IC-6");
        draft.setStatus("DRAFT");
        when(countMapper.lockById(6L)).thenReturn(draft);
        when(countMapper.transitionStatus(6L, "DRAFT", "CANCELED")).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.cancel(6L)).getCode());

        when(countMapper.selectById(404L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.get(404L)).getCode());
        service.list(null);
        service.list(" ");
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
