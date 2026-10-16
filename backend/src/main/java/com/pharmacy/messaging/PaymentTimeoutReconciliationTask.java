package com.pharmacy.messaging;

import com.pharmacy.mapper.PharmacyOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A database-backed safety net when RabbitMQ timeout messages are delayed or disabled.
 * The transactional OrderTimeoutService locks and rechecks each order before releasing stock.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutReconciliationTask {
    private static final int BATCH_SIZE = 100;
    private final PharmacyOrderMapper orders;
    private final OrderTimeoutService timeoutService;

    @Scheduled(fixedDelayString = "${app.order.timeout-reconciliation-interval-ms:60000}",
               initialDelayString = "${app.order.timeout-reconciliation-initial-delay-ms:10000}")
    public void reconcile() {
        final List<Long> expired;
        try {
            expired = orders.findExpiredPendingPaymentIds(LocalDateTime.now(), BATCH_SIZE);
        } catch (RuntimeException failure) {
            log.warn("Cannot scan expired payment orders; will retry", failure);
            return;
        }
        for (Long orderId : expired) {
            try {
                timeoutService.closeIfPending(orderId);
            } catch (RuntimeException failure) {
                log.warn("Failed to reconcile timeout for orderId={}; will retry", orderId, failure);
            }
        }
    }
}
