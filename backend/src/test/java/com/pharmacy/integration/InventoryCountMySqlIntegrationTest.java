package com.pharmacy.integration;

import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryCount;
import com.pharmacy.inventory.InventoryCountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        "spring.modulith.events.republish-outstanding-events-on-restart=false"
})
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class InventoryCountMySqlIntegrationTest {
    @Autowired private InventoryCountService countService;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM inventory_count_item");
        jdbc.update("DELETE FROM inventory_count");
        jdbc.update("DELETE FROM inventory_ledger");
        jdbc.update("DELETE FROM inventory_reservation");
        jdbc.update("DELETE FROM medicine_batch");
        jdbc.update("DELETE FROM medicine");
        jdbc.update("DELETE FROM medicine_category");
        jdbc.update("DELETE FROM sys_user_role");
        jdbc.update("DELETE FROM sys_user WHERE username LIKE 'b1_%'");
    }

    @Test
    void concurrentCompleteSucceedsOnlyOnceAndKeepsStockConsistent() throws Exception {
        Fixture fixture = seedBatch(20);
        InventoryCount count = countService.create(fixture.userId(), "B1并发盘点");
        countService.startCounting(count.getId());
        countService.upsertItem(count.getId(), fixture.batchId(), 15, "短缺5", fixture.userId());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        countService.complete(count.getId(), fixture.userId());
                        successes.incrementAndGet();
                    } catch (BusinessException ex) {
                        if (ex.getCode() == ErrorCode.STOCK_OR_STATUS_CONFLICT) {
                            conflicts.incrementAndGet();
                        } else {
                            throw ex;
                        }
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(1, successes.get());
        assertEquals(1, conflicts.get());
        assertEquals("COMPLETED", jdbc.queryForObject("SELECT status FROM inventory_count WHERE id=?", String.class, count.getId()));
        assertEquals(15, jdbc.queryForObject("SELECT available_qty FROM medicine_batch WHERE id=?", Integer.class, fixture.batchId()));
        assertEquals(15, jdbc.queryForObject("SELECT stock FROM medicine WHERE id=?", Integer.class, fixture.medicineId()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_ledger WHERE business_type='STOCK_COUNT' AND business_id=?",
                Integer.class, count.getCountNo()));
    }

    @Test
    void repeatedCompleteDoesNotReapply() {
        Fixture fixture = seedBatch(8);
        InventoryCount count = countService.create(fixture.userId(), "重复完成");
        countService.startCounting(count.getId());
        countService.upsertItem(count.getId(), fixture.batchId(), 8, "无差异", fixture.userId());
        countService.complete(count.getId(), fixture.userId());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> countService.complete(count.getId(), fixture.userId()));
        assertEquals(ErrorCode.STOCK_OR_STATUS_CONFLICT, ex.getCode());
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_ledger WHERE business_type='STOCK_COUNT' AND business_id=?",
                Integer.class, count.getCountNo()));
    }

    @Test
    void reconciliationReturnsMismatchWithoutAutoFix() {
        Fixture fixture = seedBatch(5);
        jdbc.update("UPDATE medicine SET stock=9 WHERE id=?", fixture.medicineId());

        List<Map<String, Object>> diffs = countService.reconciliation();
        assertTrue(diffs.stream().anyMatch(row -> fixture.medicineId().equals(((Number) row.get("medicineId")).longValue())));
        assertEquals(5, jdbc.queryForObject("SELECT available_qty FROM medicine_batch WHERE id=?", Integer.class, fixture.batchId()));
        assertEquals(9, jdbc.queryForObject("SELECT stock FROM medicine WHERE id=?", Integer.class, fixture.medicineId()));
    }

    private Fixture seedBatch(int qty) {
        String marker = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        jdbc.update("INSERT INTO sys_user(username,password,nickname,role,status) VALUES(?,?,?,?,1)",
                "b1_" + marker, "$2a$12$integration.test.password.hash.placeholder", "仓管", "WAREHOUSE");
        Long userId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, "b1_" + marker);
        jdbc.update("INSERT INTO medicine_category(category_name,sort_no,status,is_deleted) VALUES(?,0,1,0)", "B1分类_" + marker);
        Long categoryId = jdbc.queryForObject("SELECT id FROM medicine_category WHERE category_name=?", Long.class, "B1分类_" + marker);
        jdbc.update("""
                INSERT INTO medicine(category_id,medicine_name,price,stock,warning_stock,prescription_required,status,version,is_deleted)
                VALUES(?,?,10.00,?,?,0,1,0,0)
                """, categoryId, "B1药品_" + marker, qty, 5);
        Long medicineId = jdbc.queryForObject("SELECT id FROM medicine WHERE medicine_name=?", Long.class, "B1药品_" + marker);
        Long locationId = jdbc.queryForObject("SELECT id FROM inventory_location WHERE location_code='MAIN'", Long.class);
        jdbc.update("""
                INSERT INTO medicine_batch(medicine_id,location_id,batch_no,production_date,expiry_date,purchase_price,available_qty,reserved_qty,quality_status,sellable,version)
                VALUES(?,?,?,CURRENT_DATE,DATE_ADD(CURRENT_DATE, INTERVAL 180 DAY),?, ?,0,'QUALIFIED',1,0)
                """, medicineId, locationId, "B1_" + marker, new BigDecimal("3.00"), qty);
        Long batchId = jdbc.queryForObject("SELECT id FROM medicine_batch WHERE batch_no=?", Long.class, "B1_" + marker);
        return new Fixture(userId, medicineId, batchId);
    }

    private record Fixture(Long userId, Long medicineId, Long batchId) {
    }
}
