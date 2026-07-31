package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * TICKET-ADV132 — Persists every TradeEvent to audit_log.
 *
 * A separate consumer group ensures that audit-service receives every event
 * independently from reconciliation consumers.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditLogRepository repo;

    public AuditEventConsumer(AuditLogRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(
            topics = "trade-events",
            groupId = "audit-service"
    )
    @Transactional
    public void onTradeEvent(TradeEvent event) {
        AuditLogEntry entry = new AuditLogEntry(
                event.eventId().toString(),
                event.tradeRef(),
                event.eventType().name(),
                event.timestamp(),
                event.actor(),
                event.before(),
                event.after()
        );

        repo.save(entry);

        log.debug(
                "Audit row persisted for eventId={} tradeRef={}",
                event.eventId(),
                event.tradeRef()
        );
    }
}