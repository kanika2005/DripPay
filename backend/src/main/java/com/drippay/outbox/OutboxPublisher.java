package com.drippay.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Kafka-ready publisher: today logs events; swap logger for Kafka producer later.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;

    public OutboxPublisher(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${drippay.outbox.poll-ms:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = repository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxEvent event : pending) {
            log.info("Outbox publish type={} payload={}", event.getEventType(), event.getPayload());
            event.setStatus("PUBLISHED");
            event.setPublishedAt(Instant.now());
        }
    }
}
