package com.pharmacy.inventory;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.MedicineMapper;
import com.pharmacy.support.MybatisPlusLambdaInit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {
    @BeforeAll
    static void initLambdaCache() {
        MybatisPlusLambdaInit.entities(InventoryReservation.class);
    }

    @Mock private MedicineBatchMapper batchMapper;
    @Mock private InventoryReservationMapper reservationMapper;
    @Mock private InventoryLedgerMapper ledgerMapper;
    @Mock private MedicineMapper medicineMapper;
    @InjectMocks private InventoryServiceImpl service;

    @Test
    void reserveSplitsAcrossBatchesInMapperFefoOrder() {
        MedicineBatch first = batch(11L, 3);
        MedicineBatch second = batch(12L, 5);
        when(batchMapper.selectSellableForUpdate(eq(9L), any(LocalDate.class))).thenReturn(List.of(first, second));
        when(batchMapper.reserve(anyLong(), anyInt())).thenReturn(1);
        when(medicineMapper.decreaseStock(eq(9L), anyInt())).thenReturn(1);

        service.reserve(30L, List.of(item(20L, 9L, 7)), LocalDateTime.now().plusMinutes(30), 7L);

        verify(batchMapper).reserve(11L, 3);
        verify(batchMapper).reserve(12L, 4);
        ArgumentCaptor<InventoryReservation> captor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(reservationMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(List.of(3, 4), captor.getAllValues().stream().map(InventoryReservation::getQuantity).toList());
        verify(ledgerMapper, org.mockito.Mockito.times(2)).insert(any(InventoryLedger.class));
    }

    @Test
    void canReserveFalseWhenSellableQtyShort() {
        when(batchMapper.selectSellableForUpdate(eq(9L), any(LocalDate.class))).thenReturn(List.of(batch(11L, 2)));
        assertFalse(service.canReserve(List.of(item(1L, 9L, 3))));
        when(batchMapper.selectSellableForUpdate(eq(9L), any(LocalDate.class))).thenReturn(List.of(batch(11L, 3)));
        assertTrue(service.canReserve(List.of(item(1L, 9L, 3))));
    }

    @Test
    void reserveFailsWhenBatchUpdateLosesRace() {
        when(batchMapper.selectSellableForUpdate(eq(9L), any(LocalDate.class))).thenReturn(List.of(batch(11L, 5)));
        when(batchMapper.reserve(11L, 3)).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reserve(30L, List.of(item(20L, 9L, 3)), LocalDateTime.now(), 7L));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void reserveFailsWhenAggregateStockLosesRace() {
        when(batchMapper.selectSellableForUpdate(eq(9L), any(LocalDate.class))).thenReturn(List.of(batch(11L, 5)));
        when(batchMapper.reserve(11L, 3)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 3)).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reserve(30L, List.of(item(20L, 9L, 3)), LocalDateTime.now(), 7L));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void reserveFailsWhenBatchesExhausted() {
        when(batchMapper.selectSellableForUpdate(eq(9L), any(LocalDate.class))).thenReturn(List.of(batch(11L, 1)));
        when(batchMapper.reserve(11L, 1)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 1)).thenReturn(1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.reserve(30L, List.of(item(20L, 9L, 3)), LocalDateTime.now(), 7L));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void reserveSkipsEmptyBatchThenStopsWhenRemainingZero() {
        MedicineBatch empty = batch(10L, 0);
        MedicineBatch first = batch(11L, 3);
        MedicineBatch unused = batch(12L, 9);
        when(batchMapper.selectSellableForUpdate(eq(9L), any(LocalDate.class))).thenReturn(List.of(empty, first, unused));
        when(batchMapper.reserve(11L, 3)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 3)).thenReturn(1);

        service.reserve(30L, List.of(item(20L, 9L, 3)), LocalDateTime.now().plusMinutes(30), 7L);

        verify(batchMapper).reserve(11L, 3);
        verify(batchMapper, never()).reserve(eq(10L), anyInt());
        verify(batchMapper, never()).reserve(eq(12L), anyInt());
    }

    @Test
    void confirmSaleWritesCommitLedger() {
        InventoryReservation reservation = reservation(1L, 11L, 2, "ACTIVE");
        when(reservationMapper.activeForUpdate(30L)).thenReturn(List.of(reservation));
        when(batchMapper.selectById(11L)).thenReturn(batch(11L, 4));
        when(batchMapper.confirm(11L, 2)).thenReturn(1);
        when(reservationMapper.update(eq(null), any())).thenReturn(1);

        service.confirmSale(30L, 7L);

        ArgumentCaptor<InventoryLedger> captor = ArgumentCaptor.forClass(InventoryLedger.class);
        verify(ledgerMapper).insert(captor.capture());
        assertEquals("SALE_COMMIT", captor.getValue().getBusinessType());
    }

    @Test
    void confirmSaleFailsWhenConfirmUpdateMisses() {
        when(reservationMapper.activeForUpdate(30L)).thenReturn(List.of(reservation(1L, 11L, 2, "ACTIVE")));
        when(batchMapper.selectById(11L)).thenReturn(batch(11L, 4));
        when(batchMapper.confirm(11L, 2)).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.confirmSale(30L, 7L));
    }

    @Test
    void releaseRestoresStock() {
        when(reservationMapper.activeForUpdate(30L)).thenReturn(List.of(reservation(1L, 11L, 2, "ACTIVE")));
        when(batchMapper.selectById(11L)).thenReturn(batch(11L, 4));
        when(batchMapper.release(11L, 2)).thenReturn(1);
        when(reservationMapper.update(eq(null), any())).thenReturn(1);

        service.release(30L, "取消", 7L);

        verify(medicineMapper).restoreStock(9L, 2);
        ArgumentCaptor<InventoryLedger> captor = ArgumentCaptor.forClass(InventoryLedger.class);
        verify(ledgerMapper).insert(captor.capture());
        assertEquals("ORDER_RELEASE", captor.getValue().getBusinessType());
    }

    @Test
    void releaseFailsWhenBatchUpdateMisses() {
        when(reservationMapper.activeForUpdate(30L)).thenReturn(List.of(reservation(1L, 11L, 2, "ACTIVE")));
        when(batchMapper.selectById(11L)).thenReturn(batch(11L, 4));
        when(batchMapper.release(11L, 2)).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.release(30L, "取消", 7L)).getCode());
    }

    @Test
    void receiveCreatesBatchThenRejectsDateMismatch() {
        when(batchMapper.locationId("MAIN")).thenReturn(1L);
        when(batchMapper.findBatchForUpdate(9L, 1L, "B1")).thenReturn(null);
        when(batchMapper.insert(any(MedicineBatch.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MedicineBatch.class).setId(44L);
            return 1;
        });
        when(batchMapper.selectById(44L)).thenReturn(batch(44L, 0));
        when(batchMapper.addAvailable(44L, 5)).thenReturn(1);

        MedicineBatch created = service.receive(9L, "B1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                new BigDecimal("2.00"), 5, "PR1", 8L);
        assertEquals(44L, created.getId());
        verify(medicineMapper).restoreStock(9L, 5);

        MedicineBatch existing = batch(44L, 3);
        existing.setProductionDate(LocalDate.now().minusDays(10));
        existing.setExpiryDate(LocalDate.now().plusYears(2));
        when(batchMapper.findBatchForUpdate(9L, 1L, "B1")).thenReturn(existing);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(9L, "B1",
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), new BigDecimal("2.00"), 1, "PR2", 8L));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
    }

    @Test
    void receiveFailsWithoutMainLocation() {
        when(batchMapper.locationId("MAIN")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(9L, "B1",
                LocalDate.now(), LocalDate.now().plusDays(1), BigDecimal.ONE, 1, "PR", 8L));
        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getCode());
    }

    @Test
    void receiveAddsToExistingBatchOrSkipsZeroQty() {
        LocalDate production = LocalDate.now().minusDays(1);
        LocalDate expiry = LocalDate.now().plusYears(1);
        MedicineBatch existing = batch(44L, 3);
        existing.setProductionDate(production);
        existing.setExpiryDate(expiry);
        when(batchMapper.locationId("MAIN")).thenReturn(1L);
        when(batchMapper.findBatchForUpdate(9L, 1L, "B1")).thenReturn(existing);
        when(batchMapper.selectById(44L)).thenReturn(existing);
        when(batchMapper.addAvailable(44L, 2)).thenReturn(1);

        service.receive(9L, "B1", production, expiry, new BigDecimal("2.00"), 2, "PR3", 8L);
        verify(medicineMapper).restoreStock(9L, 2);

        service.receive(9L, "B1", production, expiry, new BigDecimal("2.00"), 0, "PR4", 8L);
        verify(batchMapper, never()).addAvailable(44L, 0);
    }

    @Test
    void adjustBatchSupportsIncrementDecrementAndSet() {
        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        when(batchMapper.adjustAvailable(11L, 3)).thenReturn(1);
        service.adjustBatch(11L, "INCREMENT", 3, "盘盈", 8L);
        verify(medicineMapper).restoreStock(9L, 3);

        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        when(batchMapper.adjustAvailable(11L, -2)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 2)).thenReturn(1);
        service.adjustBatch(11L, "DECREMENT", 2, "报损", 8L);

        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        when(batchMapper.adjustAvailable(11L, -10)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 10)).thenReturn(1);
        service.adjustBatch(11L, "SET", 0, "清零", 8L);

        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        when(batchMapper.adjustAvailable(11L, 1)).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.adjustBatch(11L, "INCREMENT", 1, "原因", 8L)).getCode());

        when(batchMapper.adjustAvailable(11L, -1)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 1)).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.adjustBatch(11L, "DECREMENT", 1, "原因", 8L)).getCode());

        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.adjustBatch(11L, null, 1, "原因", 8L)).getCode());
    }

    @Test
    void adjustBatchRejectsBlankReasonAndUnknownType() {
        BusinessException reason = assertThrows(BusinessException.class,
                () -> service.adjustBatch(11L, "INCREMENT", 1, " ", 8L));
        assertEquals(ErrorCode.PARAM_INVALID, reason.getCode());
        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        BusinessException type = assertThrows(BusinessException.class,
                () -> service.adjustBatch(11L, "ADD", 1, "原因", 8L));
        assertEquals(ErrorCode.PARAM_INVALID, type.getCode());
        when(batchMapper.lockById(99L)).thenReturn(null);
        BusinessException missing = assertThrows(BusinessException.class,
                () -> service.adjustBatch(99L, "INCREMENT", 1, "原因", 8L));
        assertEquals(ErrorCode.NOT_FOUND, missing.getCode());
    }

    @Test
    void applyStockCountSkipsZeroAndWritesLedger() {
        service.applyStockCount(11L, 0, "IC-1", "无差异", 8L);
        verify(batchMapper, never()).lockById(anyLong());

        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        when(batchMapper.adjustAvailable(11L, -2)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 2)).thenReturn(1);
        service.applyStockCount(11L, -2, "IC-1", "短缺", 8L);
        ArgumentCaptor<InventoryLedger> captor = ArgumentCaptor.forClass(InventoryLedger.class);
        verify(ledgerMapper).insert(captor.capture());
        assertEquals("STOCK_COUNT", captor.getValue().getBusinessType());
    }

    @Test
    void applyStockCountRejectsBlankMeta() {
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.applyStockCount(11L, 1, " ", "原因", 8L)).getCode());
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.applyStockCount(11L, 1, "IC", " ", 8L)).getCode());
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.applyStockCount(11L, 1, null, "原因", 8L)).getCode());
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.applyStockCount(11L, 1, "IC", null, 8L)).getCode());
    }

    @Test
    void applyStockCountIncrementsAndRejectsConflicts() {
        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        when(batchMapper.adjustAvailable(11L, 4)).thenReturn(1);
        service.applyStockCount(11L, 4, "IC-2", "盘盈", 8L);
        verify(medicineMapper).restoreStock(9L, 4);

        when(batchMapper.lockById(12L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.applyStockCount(12L, 1, "IC-2", "原因", 8L)).getCode());

        when(batchMapper.lockById(11L)).thenReturn(batch(11L, 10));
        when(batchMapper.adjustAvailable(11L, 1)).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.applyStockCount(11L, 1, "IC-2", "原因", 8L)).getCode());

        when(batchMapper.adjustAvailable(11L, -3)).thenReturn(1);
        when(medicineMapper.decreaseStock(9L, 3)).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.applyStockCount(11L, -3, "IC-2", "原因", 8L)).getCode());
    }

    @Test
    void refundRestockMarksCommittedReservation() {
        when(reservationMapper.committedForUpdate(30L)).thenReturn(List.of(reservation(1L, 11L, 2, "COMMITTED")));
        when(batchMapper.selectById(11L)).thenReturn(batch(11L, 4));
        when(batchMapper.addAvailable(11L, 2)).thenReturn(1);
        when(reservationMapper.update(eq(null), any())).thenReturn(1);
        service.refundRestock(30L, 7L);
        verify(medicineMapper).restoreStock(9L, 2);
        verify(ledgerMapper).insert(any(InventoryLedger.class));
    }

    @Test
    void refundRestockFailsWhenAddAvailableMisses() {
        when(reservationMapper.committedForUpdate(30L)).thenReturn(List.of(reservation(1L, 11L, 2, "COMMITTED")));
        when(batchMapper.selectById(11L)).thenReturn(batch(11L, 4));
        when(batchMapper.addAvailable(11L, 2)).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.refundRestock(30L, 7L)).getCode());
    }

    @Test
    void markFailureSurfacesConflict() {
        when(reservationMapper.activeForUpdate(30L)).thenReturn(List.of(reservation(1L, 11L, 2, "ACTIVE")));
        when(batchMapper.selectById(11L)).thenReturn(batch(11L, 4));
        when(batchMapper.confirm(11L, 2)).thenReturn(1);
        when(reservationMapper.update(eq(null), any())).thenReturn(0);
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT,
                assertThrows(BusinessException.class, () -> service.confirmSale(30L, 7L)).getCode());
    }

    private static PharmacyOrderItem item(Long id, Long medicineId, int qty) {
        PharmacyOrderItem item = new PharmacyOrderItem();
        item.setId(id);
        item.setMedicineId(medicineId);
        item.setMedicineName("虚构测试药品");
        item.setQuantity(qty);
        return item;
    }

    private static MedicineBatch batch(Long id, int available) {
        MedicineBatch batch = new MedicineBatch();
        batch.setId(id);
        batch.setMedicineId(9L);
        batch.setAvailableQty(available);
        batch.setReservedQty(0);
        return batch;
    }

    private static InventoryReservation reservation(Long id, Long batchId, int qty, String status) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(id);
        reservation.setBatchId(batchId);
        reservation.setQuantity(qty);
        reservation.setStatus(status);
        return reservation;
    }
}
