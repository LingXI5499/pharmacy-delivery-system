package com.pharmacy.integration;

import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.payment.PaymentAttempt;
import com.pharmacy.payment.PaymentService;
import com.pharmacy.payment.RefundRecord;
import com.pharmacy.payment.RefundService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "app.messaging.enabled=false",
        "spring.cache.type=simple",
        "spring.modulith.events.republish-outstanding-events-on-restart=false",
        "spring.datasource.hikari.maximum-pool-size=32"
})
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class PaymentRefundMySqlIntegrationTest {
    @Autowired private PaymentService paymentService;
    @Autowired private RefundService refundService;
    @Autowired private InventoryService inventoryService;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void cleanBusinessData() {
        jdbc.update("DELETE FROM inventory_ledger");
        jdbc.update("DELETE FROM inventory_reservation");
        jdbc.update("DELETE FROM order_status_log");
        jdbc.update("DELETE FROM pharmacy_order_item");
        jdbc.update("DELETE FROM refund_record");
        jdbc.update("DELETE FROM payment_attempt");
        jdbc.update("DELETE FROM prescription_item");
        jdbc.update("DELETE FROM prescription");
        jdbc.update("DELETE FROM pharmacy_order");
        jdbc.update("DELETE FROM medicine_batch");
        jdbc.update("DELETE FROM shopping_cart");
        jdbc.update("DELETE FROM medicine");
        jdbc.update("DELETE FROM medicine_category");
        jdbc.update("DELETE FROM refresh_token");
        jdbc.update("DELETE FROM sys_user_role");
        jdbc.update("DELETE FROM sys_user");
    }

    @Test
    void tenConcurrentPaymentCallbacksChangeOrderAndLedgerOnlyOnce() throws Exception {
        PaidFixture fixture = paidReadyFixture(5);
        PaymentAttempt attempt = paymentService.createAttempt(fixture.userId(), fixture.orderId());
        String sharedCallbackKey = "pay-cb-" + UUID.randomUUID();

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    paymentService.callback(fixture.userId(), attempt.getPaymentNo(), sharedCallbackKey, true, fixture.amount());
                    successes.incrementAndGet();
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(10, successes.get());
        assertEquals("TO_PACK", text("SELECT order_status FROM pharmacy_order WHERE id=?", fixture.orderId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM payment_attempt WHERE order_id=? AND status='SUCCESS'", fixture.orderId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM inventory_ledger WHERE business_type='SALE_COMMIT' AND business_id=?",
                fixture.orderId().toString()));
        assertEquals(0, quantity("SELECT available_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
        assertEquals(0, quantity("SELECT reserved_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM inventory_reservation WHERE order_id=? AND status='COMMITTED'", fixture.orderId()));
    }

    @Test
    void tenRepeatedRefundCallbacksRestockOnlyOnceBeforeShipment() throws Exception {
        PaidFixture fixture = paidAndConfirmedFixture(3);
        RefundRecord refund = refundService.request(fixture.userId(), fixture.orderId(), "发货前取消");
        assertEquals(1, refund.getRestockRequired());

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        String callbackKey = "ref-cb-" + UUID.randomUUID();
        try {
            for (int i = 0; i < 10; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    refundService.callback(refund.getRefundNo(), callbackKey, true, fixture.amount());
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals("REFUNDED", text("SELECT order_status FROM pharmacy_order WHERE id=?", fixture.orderId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM refund_record WHERE order_id=? AND status='SUCCESS'", fixture.orderId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM inventory_ledger WHERE business_type='REFUND_RESTOCK' AND business_id=?",
                fixture.orderId().toString()));
        assertEquals(3, quantity("SELECT available_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
        assertEquals(3, quantity("SELECT stock FROM medicine WHERE id=?", fixture.medicineId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM inventory_reservation WHERE order_id=? AND status='RESTOCKED'", fixture.orderId()));
    }

    @Test
    void refundCallbackDoesNotRestockAfterDeliveryStarted() {
        PaidFixture fixture = paidAndConfirmedFixture(2);
        jdbc.update("UPDATE pharmacy_order SET order_status='DELIVERING' WHERE id=?", fixture.orderId());
        RefundRecord refund = refundService.request(fixture.userId(), fixture.orderId(), "已发货仅退款");
        assertEquals(0, refund.getRestockRequired());

        for (int i = 0; i < 10; i++) {
            refundService.callback(refund.getRefundNo(), "deliver-cb", true, fixture.amount());
        }

        assertEquals("REFUNDED", text("SELECT order_status FROM pharmacy_order WHERE id=?", fixture.orderId()));
        assertEquals(0, quantity("SELECT COUNT(*) FROM inventory_ledger WHERE business_type='REFUND_RESTOCK' AND business_id=?",
                fixture.orderId().toString()));
        assertEquals(0, quantity("SELECT available_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
        assertEquals(0, quantity("SELECT stock FROM medicine WHERE id=?", fixture.medicineId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM inventory_reservation WHERE order_id=? AND status='COMMITTED'", fixture.orderId()));
    }

    @Test
    void nonOwnerIllegalAmountAndMissingPaymentAreRejected() {
        PaidFixture fixture = paidReadyFixture(1);
        PaymentAttempt attempt = paymentService.createAttempt(fixture.userId(), fixture.orderId());

        BusinessException nonOwner = assertThrows(BusinessException.class,
                () -> paymentService.callback(fixture.otherUserId(), attempt.getPaymentNo(), "own-cb", true, fixture.amount()));
        assertEquals(40401, nonOwner.getCode());

        BusinessException badAmount = assertThrows(BusinessException.class,
                () -> paymentService.callback(fixture.userId(), attempt.getPaymentNo(), "amt-cb", true, new BigDecimal("0.01")));
        assertEquals(40001, badAmount.getCode());

        BusinessException missing = assertThrows(BusinessException.class,
                () -> paymentService.callback(fixture.userId(), "PAY-NOT-EXIST", "miss-cb", true, fixture.amount()));
        assertEquals(40401, missing.getCode());

        assertEquals("PENDING_PAYMENT", text("SELECT order_status FROM pharmacy_order WHERE id=?", fixture.orderId()));
        assertEquals(0, quantity("SELECT COUNT(*) FROM inventory_ledger WHERE business_id=?", fixture.orderId().toString()));
        assertEquals("PENDING", text("SELECT status FROM payment_attempt WHERE id=?", attempt.getId()));
    }

    @Test
    void confirmSaleFailureRollsBackPaymentCallbackSideEffects() {
        PaidFixture fixture = paidReadyFixture(1);
        PaymentAttempt attempt = paymentService.createAttempt(fixture.userId(), fixture.orderId());
        // 破坏批次预占数量，使 confirm 条件更新失败并触发事务回滚
        jdbc.update("UPDATE medicine_batch SET reserved_qty=0 WHERE id=?", fixture.batchId());

        assertThrows(BusinessException.class, () ->
                paymentService.callback(fixture.userId(), attempt.getPaymentNo(), "rollback-cb", true, fixture.amount()));

        assertEquals("PENDING_PAYMENT", text("SELECT order_status FROM pharmacy_order WHERE id=?", fixture.orderId()));
        assertEquals("PENDING", text("SELECT status FROM payment_attempt WHERE id=?", attempt.getId()));
        assertEquals(0, quantity("SELECT COUNT(*) FROM inventory_ledger WHERE business_type='SALE_COMMIT' AND business_id=?",
                fixture.orderId().toString()));
        assertEquals(0, quantity("SELECT available_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
        assertEquals(0, quantity("SELECT reserved_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
        assertEquals(1, quantity("SELECT COUNT(*) FROM inventory_reservation WHERE order_id=? AND status='ACTIVE'", fixture.orderId()));
    }

    private PaidFixture paidReadyFixture(int qty) {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO sys_user(username,password,nickname,role,status) VALUES(?,?,?,?,1)",
                "pay_" + marker, "$2a$12$integration.test.password.hash.placeholder", "支付用户", "USER");
        jdbc.update("INSERT INTO sys_user(username,password,nickname,role,status) VALUES(?,?,?,?,1)",
                "oth_" + marker, "$2a$12$integration.test.password.hash.placeholder", "他人", "USER");
        Long userId = id("SELECT id FROM sys_user WHERE username=?", "pay_" + marker);
        Long otherUserId = id("SELECT id FROM sys_user WHERE username=?", "oth_" + marker);
        jdbc.update("INSERT INTO medicine_category(category_name,sort_no,status,is_deleted) VALUES(?,0,1,0)", "支付分类_" + marker);
        Long categoryId = id("SELECT id FROM medicine_category WHERE category_name=?", "支付分类_" + marker);
        BigDecimal price = new BigDecimal("10.00");
        jdbc.update("INSERT INTO medicine(category_id,medicine_name,price,stock,warning_stock,prescription_required,status,version,is_deleted) VALUES(?,?,?,?,5,0,1,0,0)",
                categoryId, "支付药品_" + marker, price, qty);
        Long medicineId = id("SELECT id FROM medicine WHERE medicine_name=?", "支付药品_" + marker);
        Long locationId = id("SELECT id FROM inventory_location WHERE location_code='MAIN'");
        jdbc.update("INSERT INTO medicine_batch(medicine_id,location_id,batch_no,production_date,expiry_date,purchase_price,available_qty,reserved_qty,quality_status,sellable,version) VALUES(?,?,?,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 1 YEAR),?,?,0,'QUALIFIED',1,0)",
                medicineId, locationId, "PAY_" + marker, new BigDecimal("5.00"), qty);
        Long batchId = id("SELECT id FROM medicine_batch WHERE batch_no=?", "PAY_" + marker);

        String orderNo = marker.substring(0, 12);
        String idem = UUID.randomUUID().toString();
        BigDecimal amount = price.multiply(BigDecimal.valueOf(qty));
        jdbc.update("INSERT INTO pharmacy_order(order_no,user_id,idempotency_key,receiver_name,receiver_phone,receiver_address,product_amount,delivery_fee,order_amount,order_status,payment_deadline) VALUES(?,?,?,?,?,?,?,?,?,'PENDING_PAYMENT',DATE_ADD(NOW(),INTERVAL 30 MINUTE))",
                orderNo, userId, idem, "收货人", "13800000000", "测试地址", amount, BigDecimal.ZERO, amount);
        Long orderId = id("SELECT id FROM pharmacy_order WHERE order_no=?", orderNo);
        jdbc.update("INSERT INTO pharmacy_order_item(order_id,medicine_id,medicine_name,medicine_price,quantity,subtotal_amount) VALUES(?,?,?,?,?,?)",
                orderId, medicineId, "支付药品_" + marker, price, qty, amount);
        Long orderItemId = id("SELECT id FROM pharmacy_order_item WHERE order_id=?", orderId);

        PharmacyOrderItem item = new PharmacyOrderItem();
        item.setId(orderItemId);
        item.setOrderId(orderId);
        item.setMedicineId(medicineId);
        item.setMedicineName("支付药品_" + marker);
        item.setMedicinePrice(price);
        item.setQuantity(qty);
        item.setSubtotalAmount(amount);
        inventoryService.reserve(orderId, List.of(item), LocalDateTime.now().plusMinutes(30), userId);

        return new PaidFixture(userId, otherUserId, medicineId, batchId, orderId, amount);
    }

    private PaidFixture paidAndConfirmedFixture(int qty) {
        PaidFixture fixture = paidReadyFixture(qty);
        PaymentAttempt attempt = paymentService.createAttempt(fixture.userId(), fixture.orderId());
        paymentService.callback(fixture.userId(), attempt.getPaymentNo(), "seed-" + UUID.randomUUID(), true, fixture.amount());
        assertEquals(OrderStatus.TO_PACK.name(), text("SELECT order_status FROM pharmacy_order WHERE id=?", fixture.orderId()));
        return fixture;
    }

    private Long id(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Long.class, arguments);
    }

    private int quantity(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Integer.class, arguments);
    }

    private String text(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, String.class, arguments);
    }

    private record PaidFixture(Long userId, Long otherUserId, Long medicineId, Long batchId, Long orderId, BigDecimal amount) {
    }
}
