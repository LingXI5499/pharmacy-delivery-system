package com.pharmacy.messaging;

import com.pharmacy.mapper.PharmacyOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTimeoutReconciliationTaskTest {
    @Mock private PharmacyOrderMapper orders;
    @Mock private OrderTimeoutService timeoutService;
    @InjectMocks private PaymentTimeoutReconciliationTask task;

    @Test
    void emptyScanDoesNotAttemptClosure() {
        when(orders.findExpiredPendingPaymentIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of());
        task.reconcile();
        verifyNoInteractions(timeoutService);
    }

    @Test
    void continuesWithOtherExpiredOrdersAfterOneFailure() {
        when(orders.findExpiredPendingPaymentIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("lock failure")).when(timeoutService).closeIfPending(1L);
        assertDoesNotThrow(task::reconcile);
        verify(timeoutService).closeIfPending(2L);
    }

    @Test
    void scanFailureDoesNotKillScheduledTask() {
        when(orders.findExpiredPendingPaymentIds(any(LocalDateTime.class), eq(100)))
                .thenThrow(new IllegalStateException("mysql unavailable"));
        assertDoesNotThrow(task::reconcile);
    }
}
