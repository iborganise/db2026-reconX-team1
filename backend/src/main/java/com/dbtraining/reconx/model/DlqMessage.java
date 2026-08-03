package com.dbtraining.reconx.model;

import com.dbtraining.reconx.dto.TradeEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dlq_messages")
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private UUID eventId;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(nullable = false)
    private Integer partition;

    @Column(nullable = false)
    private Long offset;

    @Convert(converter = TradeEventJsonConverter.class)
    @Column(name = "payload", nullable = false, length = 1000000, columnDefinition = "VARCHAR(1000000)")
    private TradeEvent payload;

    @Column(length = 1000)
    private String reason;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    public DlqMessage() {
    }

    public DlqMessage(UUID eventId, String tradeRef, String originalTopic,
                      Integer partition, Long offset, TradeEvent payload,
                      String reason, Instant firstSeen) {
        this.eventId = eventId;
        this.tradeRef = tradeRef;
        this.originalTopic = originalTopic;
        this.partition = partition;
        this.offset = offset;
        this.payload = payload;
        this.reason = reason;
        this.firstSeen = firstSeen;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getTradeRef() {
        return tradeRef;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public Integer getPartition() {
        return partition;
    }

    public Long getOffset() {
        return offset;
    }

    public TradeEvent getPayload() {
        return payload;
    }

    public String getReason() {
        return reason;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }
}
