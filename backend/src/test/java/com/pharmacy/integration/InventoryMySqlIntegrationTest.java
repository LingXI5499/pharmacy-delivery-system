package com.pharmacy.integration;

import com.pharmacy.entity.PharmacyOrderItem;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
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
class InventoryMySqlIntegrationTest {
    @Autowired InventoryService inventoryService;
    @Autowired JdbcTemplate jdbc;

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
    void oneHundredConcurrentReservationsNeverOversellFiftyUnits() throws Exception {
        Fixture fixture = fixture(50, true);
        List<OrderItemRef> orders = new ArrayList<>();
        for (int index = 0; index < 100; index++) orders.add(orderItem(fixture, 1));

        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (OrderItemRef ref : orders) futures.add(pool.submit(() -> {
                start.await();
                try {
                    inventoryService.reserve(ref.orderId(), List.of(ref.item()), LocalDateTime.now().plusMinutes(30), fixture.userId());
                    return true;
                } catch (BusinessException expectedWhenSoldOut) {
                    return false;
                }
            }));
            start.countDown();
            int successes = 0;
            for (Future<Boolean> future : futures) if (future.get(60, TimeUnit.SECONDS)) successes++;

            assertEquals(50, successes);
            assertEquals(0, quantity("SELECT available_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
            assertEquals(50, quantity("SELECT reserved_qty FROM medicine_batch WHERE id=?", fixture.batchId()));
            assertEquals(0, quantity("SELECT stock FROM medicine WHERE id=?", fixture.medicineId()));
            assertEquals(50, quantity("SELECT COUNT(*) FROM inventory_reservation WHERE status='ACTIVE'"));
            assertEquals(50, quantity("SELECT COUNT(*) FROM inventory_ledger WHERE business_type='ORDER_RESERVE'"));
            assertEquals(0, quantity("SELECT COUNT(*) FROM medicine_batch WHERE available_qty<0 OR reserved_qty<0"));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void failureOnLaterItemRollsBackEarlierReservationAndLedger() {
        Fixture available = fixture(1, true);
        Fixture unavailable = fixture(0, false);
        OrderItemRef first = orderItem(available, 1);
        PharmacyOrderItem second = item(first.orderId(), unavailable.medicineId(), 1);

        assertThrows(BusinessException.class, () -> inventoryService.reserve(
                first.orderId(), List.of(first.item(), second), LocalDateTime.now().plusMinutes(30), available.userId()));

        assertEquals(1, quantity("SELECT available_qty FROM medicine_batch WHERE id=?", available.batchId()));
        assertEquals(0, quantity("SELECT reserved_qty FROM medicine_batch WHERE id=?", available.batchId()));
        assertEquals(1, quantity("SELECT stock FROM medicine WHERE id=?", available.medicineId()));
        assertEquals(0, quantity("SELECT COUNT(*) FROM inventory_reservation WHERE order_id=?", first.orderId()));
        assertEquals(0, quantity("SELECT COUNT(*) FROM inventory_ledger WHERE business_id=?", first.orderId().toString()));
    }

    private Fixture fixture(int stock, boolean createBatch) {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jdbc.update("INSERT INTO sys_user(username,password,nickname,role,status) VALUES(?,?,?,?,1)", "it_" + marker, "$2a$12$integration.test.password.hash.placeholder", "集成测试", "USER");
        Long userId = id("SELECT id FROM sys_user WHERE username=?", "it_" + marker);
        jdbc.update("INSERT INTO medicine_category(category_name,sort_no,status,is_deleted) VALUES(?,0,1,0)", "集成分类_" + marker);
        Long categoryId = id("SELECT id FROM medicine_category WHERE category_name=?", "集成分类_" + marker);
        jdbc.update("INSERT INTO medicine(category_id,medicine_name,price,stock,warning_stock,prescription_required,status,version,is_deleted) VALUES(?,?,?,?,5,0,1,0,0)", categoryId, "集成药品_" + marker, new BigDecimal("10.00"), stock);
        Long medicineId = id("SELECT id FROM medicine WHERE medicine_name=?", "集成药品_" + marker);
        Long batchId = null;
        if (createBatch) {
            Long locationId = id("SELECT id FROM inventory_location WHERE location_code='MAIN'");
            jdbc.update("INSERT INTO medicine_batch(medicine_id,location_id,batch_no,production_date,expiry_date,purchase_price,available_qty,reserved_qty,quality_status,sellable,version) VALUES(?,?,?,CURRENT_DATE,DATE_ADD(CURRENT_DATE,INTERVAL 1 YEAR),?,?,0,'QUALIFIED',1,0)", medicineId, locationId, "IT_" + marker, new BigDecimal("5.00"), stock);
            batchId = id("SELECT id FROM medicine_batch WHERE batch_no=?", "IT_" + marker);
        }
        return new Fixture(userId, categoryId, medicineId, batchId);
    }

    private OrderItemRef orderItem(Fixture fixture, int quantity) {
        String marker = UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO pharmacy_order(order_no,user_id,idempotency_key,receiver_name,receiver_phone,receiver_address,product_amount,delivery_fee,order_amount,order_status) VALUES(?,?,?,?,?,?,?,?,?,'PENDING_PAYMENT')",
                marker.substring(0, 32), fixture.userId(), marker, "测试收货人", "13800000000", "测试地址", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        Long orderId = id("SELECT id FROM pharmacy_order WHERE order_no=?", marker.substring(0, 32));
        PharmacyOrderItem item = item(orderId, fixture.medicineId(), quantity);
        jdbc.update("INSERT INTO pharmacy_order_item(order_id,medicine_id,medicine_name,medicine_price,quantity,subtotal_amount) VALUES(?,?,?,?,?,?)",
                orderId, item.getMedicineId(), item.getMedicineName(), item.getMedicinePrice(), quantity, item.getSubtotalAmount());
        item.setId(id("SELECT id FROM pharmacy_order_item WHERE order_id=?", orderId));
        return new OrderItemRef(orderId, item);
    }

    private PharmacyOrderItem item(Long orderId, Long medicineId, int quantity) {
        PharmacyOrderItem item = new PharmacyOrderItem();
        item.setOrderId(orderId);
        item.setMedicineId(medicineId);
        item.setMedicineName("并发测试药品");
        item.setMedicinePrice(BigDecimal.TEN);
        item.setQuantity(quantity);
        item.setSubtotalAmount(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private Long id(String sql, Object... arguments) { return jdbc.queryForObject(sql, Long.class, arguments); }
    private int quantity(String sql, Object... arguments) { return jdbc.queryForObject(sql, Integer.class, arguments); }
    private record Fixture(Long userId, Long categoryId, Long medicineId, Long batchId) {}
    private record OrderItemRef(Long orderId, PharmacyOrderItem item) {}
}
