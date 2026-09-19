package com.pharmacy.controller;

import com.pharmacy.common.PageData;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.GlobalExceptionHandler;
import com.pharmacy.inventory.InventoryCount;
import com.pharmacy.inventory.InventoryCountService;
import com.pharmacy.inventory.InventoryLedgerMapper;
import com.pharmacy.inventory.InventoryQueryController;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.MedicineBatch;
import com.pharmacy.inventory.MedicineBatchMapper;
import com.pharmacy.observability.PharmacyBusinessMetrics;
import com.pharmacy.payment.PaymentAttempt;
import com.pharmacy.payment.PaymentController;
import com.pharmacy.payment.PaymentService;
import com.pharmacy.payment.RefundController;
import com.pharmacy.payment.RefundRecord;
import com.pharmacy.payment.RefundService;
import com.pharmacy.procurement.ProcurementController;
import com.pharmacy.procurement.ProcurementService;
import com.pharmacy.procurement.PurchaseOrder;
import com.pharmacy.procurement.Supplier;
import com.pharmacy.security.AuthenticatedUser;
import com.pharmacy.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WarehouseAdminMvcTest {
    private MedicineBatchMapper batches;
    private InventoryLedgerMapper ledger;
    private InventoryService inventory;
    private InventoryCountService countService;
    private ProcurementService procurement;
    private PaymentService payments;
    private RefundService refunds;
    private OrderService orders;
    private MockMvc inventoryMvc;
    private MockMvc procurementMvc;
    private MockMvc paymentMvc;
    private MockMvc refundMvc;
    private MockMvc adminOrderMvc;

    @BeforeEach
    void setup() {
        batches = mock(MedicineBatchMapper.class);
        ledger = mock(InventoryLedgerMapper.class);
        inventory = mock(InventoryService.class);
        countService = mock(InventoryCountService.class);
        procurement = mock(ProcurementService.class);
        payments = mock(PaymentService.class);
        refunds = mock(RefundService.class);
        orders = mock(OrderService.class);
        GlobalExceptionHandler advice = new GlobalExceptionHandler(mock(PharmacyBusinessMetrics.class));
        inventoryMvc = MockMvcBuilders.standaloneSetup(new InventoryQueryController(batches, ledger, inventory, countService))
                .setControllerAdvice(advice).build();
        procurementMvc = MockMvcBuilders.standaloneSetup(new ProcurementController(procurement))
                .setControllerAdvice(advice).build();
        paymentMvc = MockMvcBuilders.standaloneSetup(new PaymentController(payments))
                .setControllerAdvice(advice).build();
        refundMvc = MockMvcBuilders.standaloneSetup(new RefundController(refunds))
                .setControllerAdvice(advice).build();
        adminOrderMvc = MockMvcBuilders.standaloneSetup(new AdminOrderController(orders))
                .setControllerAdvice(advice).build();
        AuthenticatedUser user = new AuthenticatedUser(7L, "wh", "仓库", UserRole.WAREHOUSE);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nearExpiryWindowMustBe30Or60Or90() throws Exception {
        inventoryMvc.perform(get("/api/warehouse/inventory/batches").param("expiringDays", "15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void batchesLedgerCountsAndAdjust() throws Exception {
        when(batches.selectList(any())).thenReturn(List.of(new MedicineBatch()));
        inventoryMvc.perform(get("/api/warehouse/inventory/batches").param("expiringDays", "30"))
                .andExpect(status().isOk());
        inventoryMvc.perform(get("/api/warehouse/inventory/ledger").param("batchId", "1"))
                .andExpect(status().isOk());
        when(countService.reconciliation()).thenReturn(List.of());
        inventoryMvc.perform(get("/api/warehouse/inventory/reconciliation")).andExpect(status().isOk());
        when(countService.list(null)).thenReturn(List.of());
        inventoryMvc.perform(get("/api/warehouse/inventory/counts")).andExpect(status().isOk());
        when(countService.detail(9L)).thenReturn(java.util.Map.of("count", new InventoryCount()));
        inventoryMvc.perform(get("/api/warehouse/inventory/counts/9")).andExpect(status().isOk());
        when(countService.create(eq(7L), isNull())).thenReturn(new InventoryCount());
        inventoryMvc.perform(post("/api/warehouse/inventory/counts")).andExpect(status().isOk());
        inventoryMvc.perform(post("/api/warehouse/inventory/counts/9/start")).andExpect(status().isOk());
        verify(countService).startCounting(9L);
        inventoryMvc.perform(put("/api/warehouse/inventory/counts/9/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchId\":5,\"countedQty\":8,\"reason\":\"短缺\"}"))
                .andExpect(status().isOk());
        inventoryMvc.perform(post("/api/warehouse/inventory/counts/9/complete")).andExpect(status().isOk());
        inventoryMvc.perform(post("/api/warehouse/inventory/counts/9/cancel")).andExpect(status().isOk());
        inventoryMvc.perform(patch("/api/warehouse/inventory/batches/5/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adjustType\":\"INCREMENT\",\"quantity\":1,\"reason\":\"验收\"}"))
                .andExpect(status().isOk());
        verify(inventory).adjustBatch(5L, "INCREMENT", 1, "验收", 7L);
    }

    @Test
    void procurementAndPaymentRefundAdminOrder() throws Exception {
        when(procurement.suppliers()).thenReturn(List.of(new Supplier()));
        procurementMvc.perform(get("/api/purchaser/suppliers")).andExpect(status().isOk());
        procurementMvc.perform(post("/api/purchaser/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierCode\":\"S1\",\"supplierName\":\"虚构供应商\"}"))
                .andExpect(status().isOk());
        procurementMvc.perform(post("/api/purchaser/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierId\":1,\"items\":[{\"medicineId\":9,\"quantity\":2,\"purchasePrice\":1.50}]}"))
                .andExpect(status().isOk());
        procurementMvc.perform(get("/api/purchaser/purchase-orders")).andExpect(status().isOk());
        procurementMvc.perform(get("/api/purchaser/purchase-orders/1")).andExpect(status().isOk());
        procurementMvc.perform(get("/api/admin/purchase-orders")).andExpect(status().isOk());
        procurementMvc.perform(get("/api/admin/purchase-orders/1")).andExpect(status().isOk());
        procurementMvc.perform(post("/api/admin/purchase-orders/1/approve")).andExpect(status().isOk());
        procurementMvc.perform(post("/api/admin/purchase-orders/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"质量问题\"}"))
                .andExpect(status().isOk());
        procurementMvc.perform(get("/api/warehouse/purchase-orders")).andExpect(status().isOk());
        procurementMvc.perform(get("/api/warehouse/purchase-orders/1")).andExpect(status().isOk());

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setPaymentNo("PAY1");
        when(payments.createAttempt(7L, 3L)).thenReturn(attempt);
        paymentMvc.perform(post("/api/user/orders/3/payments")).andExpect(status().isOk());
        paymentMvc.perform(post("/api/user/mock-payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentNo\":\"PAY1\",\"callbackKey\":\"cb\",\"success\":true,\"amount\":10}"))
                .andExpect(status().isOk());

        RefundRecord refund = new RefundRecord();
        refund.setRefundNo("REF1");
        when(refunds.request(eq(7L), eq(3L), anyString())).thenReturn(refund);
        refundMvc.perform(post("/api/user/orders/3/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"不想要了\"}"))
                .andExpect(status().isOk());
        refundMvc.perform(post("/api/admin/mock-refunds/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refundNo\":\"REF1\",\"callbackKey\":\"rcb\",\"success\":true,\"amount\":10}"))
                .andExpect(status().isOk());

        when(orders.pageAdminOrders(anyLong(), anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageData<>(List.of(), 0, 1, 10, 0));
        adminOrderMvc.perform(get("/api/admin/orders")).andExpect(status().isOk());
        adminOrderMvc.perform(get("/api/admin/orders/3")).andExpect(status().isOk());
        adminOrderMvc.perform(post("/api/admin/orders/3/accept")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        adminOrderMvc.perform(post("/api/admin/orders/3/pack")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        adminOrderMvc.perform(post("/api/admin/orders/3/dispatch")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"riderId\":2}"))
                .andExpect(status().isOk());
        adminOrderMvc.perform(post("/api/admin/orders/3/complete")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        adminOrderMvc.perform(post("/api/admin/orders/3/cancel")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"缺货\"}"))
                .andExpect(status().isOk());
    }
}
