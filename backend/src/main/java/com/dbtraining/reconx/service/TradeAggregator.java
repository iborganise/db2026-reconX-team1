package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * TICKET-ADV137 — Event sourcing rebuild:
 * Folds every AuditLogEntry for a given tradeRef in chronological order to
 * reconstruct the current trade state.
 */
@Service
public class TradeAggregator {

    private final AuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;

    public TradeAggregator(AuditLogRepository auditRepo, ObjectMapper objectMapper) {
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> rebuild(String tradeRef) {
        List<AuditLogEntry> events = auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        JsonNode state = null;
        for (AuditLogEntry e : events) {
            try {
                TradeEvent.EventType type = TradeEvent.EventType.valueOf(e.getEventType());
                switch (type) {
                    case TRADE_CREATED, TRADE_UPDATED -> {
                        if (e.getAfterState() != null) {
                            state = objectMapper.readTree(e.getAfterState());
                        }
                    }
                    case TRADE_CANCELLED -> state = null;
                }
            } catch (Exception ex) {
                // If eventType string parsing or JSON parsing fails, skip or retain previous state
            }
        }
        return Optional.ofNullable(state);
    }
}
