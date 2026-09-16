package com.pharmacy.messaging;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReliableEventPublisherTest {
    @Test
    void expiredDeadlineUsesMinimumTtl() {
        assertEquals("1", ReliableEventPublisher.expirationMillis(null));
        assertEquals("1", ReliableEventPublisher.expirationMillis(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    void futureDeadlineUsesRemainingMillisCappedAtThirtyMinutes() {
        long tenSeconds = Long.parseLong(ReliableEventPublisher.expirationMillis(LocalDateTime.now().plusSeconds(10)));
        assertTrue(tenSeconds >= 10_000 && tenSeconds <= 13_000, "remaining ttl was " + tenSeconds);
        assertEquals(
                String.valueOf(30L * 60 * 1000),
                ReliableEventPublisher.expirationMillis(LocalDateTime.now().plusHours(2)));
    }
}
