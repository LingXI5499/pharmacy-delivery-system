package com.pharmacy.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PharmacyBusinessMetrics {
    private final Counter orderBusinessFailures;
    private final Counter inventoryConflicts;
    private final Counter authFailures;

    public PharmacyBusinessMetrics(MeterRegistry registry) {
        this.orderBusinessFailures = Counter.builder("pharmacy_order_business_failures_total")
                .description("Order/payment related business conflicts and failures")
                .register(registry);
        this.inventoryConflicts = Counter.builder("pharmacy_inventory_conflicts_total")
                .description("Inventory stock or reservation conflicts")
                .register(registry);
        this.authFailures = Counter.builder("pharmacy_auth_failures_total")
                .description("Authentication and authorization failures")
                .register(registry);
    }

    public void recordErrorCode(int errorCode) {
        if (errorCode == 40901) {
            inventoryConflicts.increment();
        } else if (errorCode == 40902 || errorCode == 40904) {
            orderBusinessFailures.increment();
        } else if (errorCode == 40101 || errorCode == 40301) {
            authFailures.increment();
        } else if (errorCode >= 50000) {
            orderBusinessFailures.increment();
        }
    }
}
