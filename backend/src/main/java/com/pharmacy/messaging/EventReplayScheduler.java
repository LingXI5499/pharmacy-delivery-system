package com.pharmacy.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class EventReplayScheduler {
    private final IncompleteEventPublications publications;

    @Scheduled(
            fixedDelayString = "${app.messaging.replay-interval-ms:60000}",
            initialDelayString = "${app.messaging.replay-initial-delay-ms:30000}")
    public void replay() {
        try {
            publications.resubmitIncompletePublicationsOlderThan(Duration.ofSeconds(5));
        } catch (RuntimeException failure) {
            log.warn("Outstanding domain events are still waiting for their broker", failure);
        }
    }
}
