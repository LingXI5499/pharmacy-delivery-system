package com.pharmacy.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutstandingEventMetricsScheduler {
    private final OutstandingEventMetrics metrics;

    @Scheduled(fixedDelayString = "${app.observability.event-gauge-refresh-ms:30000}")
    public void refresh() {
        metrics.refresh();
    }
}
