package com.pharmacy.procurement;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.Medicine;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.MedicineBatch;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class ProcurementServiceTest {
    @BeforeAll
    static void initLambdaCache() {
        MybatisPlusLambdaInit.entities(PurchaseOrder.class, PurchaseOrderItem.class, PurchaseReceipt.class, PurchaseReceiptItem.class, Supplier.class);
    }

    @Mock private SupplierMapper supplierMapper;
    @Mock private PurchaseOrderMapper orderMapper;
    @Mock private PurchaseOrderItemMapper itemMapper;
    @Mock private PurchaseReceiptMapper receiptMapper;
    @Mock private PurchaseReceiptItemMapper receiptItemMapper;
    @Mock private InventoryService inventoryService;
    @Mock private MedicineMapper medicineMapper;
    @InjectMocks private ProcurementService service;

    @Test
    void createSupplierPersistsActiveRow() {
        when(supplierMapper.insert(any(Supplier.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Supplier.class).setId(1L);
            return 1;
        });
        Supplier saved = service.createSupplier(new SupplierRequest("S1", "虚构供应商", "李联系", "13800000002"));
        assertEquals(1, saved.getStatus());
        assertEquals("S1", saved.getSupplierCode());
    }

    @Test
    void createPurchaseRejectsDisabledSupplier() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setStatus(0);
        when(supplierMapper.selectById(1L)).thenReturn(supplier);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(9L, new PurchaseCreateRequest(1L,
                        List.of(new PurchaseItemRequest(11L, 10, new BigDecimal("3.00"))), null)));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getCode());
        verify(orderMapper, never()).insert(any(PurchaseOrder.class));
    }

    @Test
    void createPurchaseInsertsDraftItems() {
        Supplier supplier = activeSupplier();
        when(supplierMapper.selectById(1L)).thenReturn(supplier);
        when(orderMapper.insert(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseOrder.class).setId(20L);
            return 1;
        });
        PurchaseOrder order = service.create(9L, new PurchaseCreateRequest(1L,
                List.of(new PurchaseItemRequest(11L, 10, new BigDecimal("3.00"))), "补货"));
        assertEquals("DRAFT", order.getStatus());
        ArgumentCaptor<PurchaseOrderItem> captor = ArgumentCaptor.forClass(PurchaseOrderItem.class);
        verify(itemMapper).insert(captor.capture());
        assertEquals(20L, captor.getValue().getPurchaseOrderId());
        assertEquals(0, captor.getValue().getReceivedQty());
    }

    @Test
    void approveRejectsNonDraft() {
        PurchaseOrder order = purchase(20L, "APPROVED");
        when(orderMapper.lockById(20L)).thenReturn(order);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(1L, 20L));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void approveUpdatesDraft() {
        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "DRAFT"));
        service.approve(1L, 20L);
        verify(orderMapper).update(eq(null), any());
    }

    @Test
    void rejectRequiresReasonAndDraft() {
        BusinessException blank = assertThrows(BusinessException.class, () -> service.reject(1L, 20L, "  "));
        assertEquals(ErrorCode.PARAM_INVALID, blank.getCode());

        when(orderMapper.lockById(20L)).thenReturn(null);
        BusinessException missing = assertThrows(BusinessException.class, () -> service.reject(1L, 20L, "质量问题"));
        assertEquals(ErrorCode.NOT_FOUND, missing.getCode());

        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "APPROVED"));
        BusinessException status = assertThrows(BusinessException.class, () -> service.reject(1L, 20L, "质量问题"));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT, status.getCode());
    }

    @Test
    void rejectAppendsReasonAndDetectsRace() {
        PurchaseOrder order = purchase(20L, "DRAFT");
        order.setRemark("原备注");
        when(orderMapper.lockById(20L)).thenReturn(order);
        when(orderMapper.update(eq(null), any())).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.reject(1L, 20L, "质量问题"));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT, ex.getCode());
    }

    @Test
    void rejectSucceedsOnDraft() {
        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "DRAFT"));
        when(orderMapper.update(eq(null), any())).thenReturn(1);
        service.reject(1L, 20L, "质量问题");
        verify(orderMapper).update(eq(null), any());
    }

    @Test
    void receiveRejectsWrongStatusAndForeignItem() {
        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "DRAFT"));
        ReceiptRequest request = receiptRequest(20L, 30L, 5, 0);
        BusinessException status = assertThrows(BusinessException.class, () -> service.receive(8L, request));
        assertEquals(ErrorCode.ORDER_STATUS_CONFLICT, status.getCode());

        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "APPROVED"));
        when(receiptMapper.insert(any(PurchaseReceipt.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseReceipt.class).setId(70L);
            return 1;
        });
        PurchaseOrderItem foreign = line(30L, 99L, 11L, 10, 0);
        when(itemMapper.selectById(30L)).thenReturn(foreign);
        BusinessException item = assertThrows(BusinessException.class, () -> service.receive(8L, request));
        assertEquals(ErrorCode.PARAM_INVALID, item.getCode());
    }

    @Test
    void receiveRejectsOverQuantity() {
        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "APPROVED"));
        when(receiptMapper.insert(any(PurchaseReceipt.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseReceipt.class).setId(70L);
            return 1;
        });
        when(itemMapper.selectById(30L)).thenReturn(line(30L, 20L, 11L, 10, 0));
        when(itemMapper.addReceived(30L, 10)).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.receive(8L, receiptRequest(20L, 30L, 10, 0)));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
        verify(inventoryService, never()).receive(anyLong(), any(), any(), any(), any(), anyInt(), any(), anyLong());
    }

    @Test
    void receiveMarksPartiallyThenFullyReceived() {
        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "APPROVED"));
        when(receiptMapper.insert(any(PurchaseReceipt.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseReceipt.class).setId(70L);
            return 1;
        });
        when(itemMapper.selectById(30L)).thenReturn(line(30L, 20L, 11L, 10, 0));
        when(itemMapper.addReceived(30L, 4)).thenReturn(1);
        MedicineBatch batch = new MedicineBatch();
        batch.setId(88L);
        when(inventoryService.receive(eq(11L), eq("B1"), any(), any(), any(), eq(4), any(), eq(8L))).thenReturn(batch);
        when(itemMapper.selectList(any())).thenReturn(List.of(line(30L, 20L, 11L, 10, 4)));

        PurchaseReceipt receipt = service.receive(8L, receiptRequest(20L, 30L, 4, 0));
        assertEquals(70L, receipt.getId());
        verify(receiptItemMapper).insert(any(PurchaseReceiptItem.class));
        verify(orderMapper).update(eq(null), any());
    }

    @Test
    void detailMapsMedicineNameAndRejectReason() {
        PurchaseOrder order = purchase(20L, "REJECTED");
        order.setRemark("原备注 | [REJECTED] 质量问题");
        when(orderMapper.selectById(20L)).thenReturn(order);
        when(supplierMapper.selectById(1L)).thenReturn(activeSupplier());
        when(itemMapper.selectList(any())).thenReturn(List.of(line(30L, 20L, 11L, 10, 3)));
        Medicine medicine = new Medicine();
        medicine.setId(11L);
        medicine.setMedicineName("维生素C");
        when(medicineMapper.selectById(11L)).thenReturn(medicine);

        PurchaseOrderDetail detail = service.detail(20L);
        assertEquals("维生素C", detail.items().get(0).medicineName());
        assertEquals(7, detail.items().get(0).unreceivedQty());
        assertEquals("质量问题", detail.rejectReason());
    }

    @Test
    void listAndReceivableCoverStatusFilter() {
        when(orderMapper.selectList(any())).thenReturn(List.of());
        assertTrue(service.list("DRAFT").isEmpty());
        assertTrue(service.list("  ").isEmpty());
        assertTrue(service.listReceivable().isEmpty());
        assertTrue(service.suppliers().isEmpty());
    }

    @Test
    void listMapsMissingSupplierAndReceivesFully() {
        PurchaseOrder draft = purchase(20L, "DRAFT");
        when(orderMapper.selectList(any())).thenReturn(List.of(draft));
        when(supplierMapper.selectById(1L)).thenReturn(null);
        assertNull(service.list(null).get(0).supplierName());
        when(supplierMapper.selectById(1L)).thenReturn(activeSupplier());
        assertEquals("虚构供应商", service.list("DRAFT").get(0).supplierName());

        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "PARTIALLY_RECEIVED"));
        when(receiptMapper.insert(any(PurchaseReceipt.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseReceipt.class).setId(71L);
            return 1;
        });
        when(itemMapper.selectById(30L)).thenReturn(line(30L, 20L, 11L, 10, 4));
        when(itemMapper.addReceived(30L, 6)).thenReturn(1);
        MedicineBatch batch = new MedicineBatch();
        batch.setId(88L);
        when(inventoryService.receive(eq(11L), eq("B1"), any(), any(), any(), eq(6), any(), eq(8L))).thenReturn(batch);
        when(itemMapper.selectList(any())).thenReturn(List.of(line(30L, 20L, 11L, 10, 10)));
        service.receive(8L, receiptRequest(20L, 30L, 6, 0));
        verify(orderMapper, org.mockito.Mockito.atLeastOnce()).update(eq(null), any());
    }

    @Test
    void approveAndReceiveRejectMissingOrder() {
        when(orderMapper.lockById(20L)).thenReturn(null);
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.approve(1L, 20L)).getCode());
        assertEquals(ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.receive(8L, receiptRequest(20L, 30L, 1, 0))).getCode());
    }

    @Test
    void detailFallsBackWhenMedicineMissing() {
        when(orderMapper.selectById(20L)).thenReturn(purchase(20L, "APPROVED"));
        when(supplierMapper.selectById(1L)).thenReturn(null);
        PurchaseOrderItem item = line(30L, 20L, 11L, 10, 0);
        item.setOrderedQty(null);
        item.setReceivedQty(null);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(medicineMapper.selectById(11L)).thenReturn(null);
        PurchaseOrderDetail detail = service.detail(20L);
        assertEquals("药品#11", detail.items().get(0).medicineName());
        assertEquals(0, detail.items().get(0).unreceivedQty());
        assertNull(detail.supplierName());
    }

    @Test
    void createPurchaseRejectsMissingSupplier() {
        when(supplierMapper.selectById(1L)).thenReturn(null);
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.create(9L, new PurchaseCreateRequest(1L,
                        List.of(new PurchaseItemRequest(11L, 10, new BigDecimal("3.00"))), null))).getCode());
    }

    @Test
    void receiveRejectsNullLine() {
        when(orderMapper.lockById(20L)).thenReturn(purchase(20L, "APPROVED"));
        when(receiptMapper.insert(any(PurchaseReceipt.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, PurchaseReceipt.class).setId(70L);
            return 1;
        });
        when(itemMapper.selectById(30L)).thenReturn(null);
        assertEquals(ErrorCode.PARAM_INVALID,
                assertThrows(BusinessException.class, () -> service.receive(8L, receiptRequest(20L, 30L, 1, 0))).getCode());
    }

    @Test
    void detailMissingOrder() {
        when(orderMapper.selectById(20L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.detail(20L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getCode());
    }

    @Test
    void extractRejectReasonFallsBackToRemark() {
        PurchaseOrder order = purchase(20L, "REJECTED");
        order.setRemark("手工拒绝");
        when(orderMapper.selectById(20L)).thenReturn(order);
        when(supplierMapper.selectById(1L)).thenReturn(activeSupplier());
        when(itemMapper.selectList(any())).thenReturn(List.of());
        assertEquals("手工拒绝", service.detail(20L).rejectReason());

        PurchaseOrder approved = purchase(21L, "APPROVED");
        approved.setRemark("普通备注");
        when(orderMapper.selectById(21L)).thenReturn(approved);
        assertNull(service.detail(21L).rejectReason());
    }

    private static Supplier activeSupplier() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setStatus(1);
        supplier.setSupplierName("虚构供应商");
        return supplier;
    }

    private static PurchaseOrder purchase(Long id, String status) {
        PurchaseOrder order = new PurchaseOrder();
        order.setId(id);
        order.setPurchaseNo("PO" + id);
        order.setSupplierId(1L);
        order.setStatus(status);
        return order;
    }

    private static PurchaseOrderItem line(Long id, Long orderId, Long medicineId, int ordered, int received) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setId(id);
        item.setPurchaseOrderId(orderId);
        item.setMedicineId(medicineId);
        item.setOrderedQty(ordered);
        item.setReceivedQty(received);
        item.setPurchasePrice(new BigDecimal("3.00"));
        return item;
    }

    private static ReceiptRequest receiptRequest(Long orderId, Long itemId, int qualified, int rejected) {
        return new ReceiptRequest(orderId, List.of(new ReceiptItemRequest(
                itemId, "B1", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1), qualified, rejected)), null);
    }
}
