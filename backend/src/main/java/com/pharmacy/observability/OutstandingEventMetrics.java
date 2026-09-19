package com.pharmacy.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Exposes unfinished Modulith event publications for Grafana/Prometheus.
 */
@Component
public class OutstandingEventMetrics {
    private final JdbcTemplate jdbc;
    private final AtomicLong outstanding = new AtomicLong(0);

    public OutstandingEventMetrics(MeterRegistry registry, JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        Gauge.builder("pharmacy_outstanding_events", outstanding, AtomicLong::get)
                .description("Uncompleted rows in EVENT_PUBLICATION")
                .register(registry);
        refresh();
    }

    public void refresh() {
        try {
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE completion_date IS NULL",
                    Long.class);
            outstanding.set(count == null ? 0L : count);
        } catch (RuntimeException ignored) {
            // Table may be unavailable during early startup; keep last value.
        }
    }
}
