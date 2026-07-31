package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditLogRepository repo;

    @InjectMocks
    private AuditEventConsumer consumer;

    @Test
    void persistsTradeEventAsAuditLogEntry() {
        UUID eventId = UUID.fromString(
                "f5e2e05b-82f6-44f0-9db4-c61c7d29f940"
        );

        Instant timestamp =
                Instant.parse("2026-07-31T12:00:00Z");

        TradeEvent event = new TradeEvent(
                eventId,
                "TRD-20260731-0132",
                TradeEvent.EventType.TRADE_UPDATED,
                timestamp,
                "trader@db.com",
                "{\"status\":\"PENDING\"}",
                "{\"status\":\"MATCHED\"}"
        );

        consumer.onTradeEvent(event);

        ArgumentCaptor<AuditLogEntry> captor =
                ArgumentCaptor.forClass(AuditLogEntry.class);

        verify(repo).save(captor.capture());

        AuditLogEntry saved = captor.getValue();

        assertAll(
                () -> assertEquals(
                        eventId.toString(),
                        saved.getEventId()
                ),
                () -> assertEquals(
                        "TRD-20260731-0132",
                        saved.getTradeRef()
                ),
                () -> assertEquals(
                        "TRADE_UPDATED",
                        saved.getEventType()
                ),
                () -> assertEquals(
                        timestamp,
                        saved.getEventTimestamp()
                ),
                () -> assertEquals(
                        "trader@db.com",
                        saved.getActor()
                ),
                () -> assertEquals(
                        "{\"status\":\"PENDING\"}",
                        saved.getBeforeState()
                ),
                () -> assertEquals(
                        "{\"status\":\"MATCHED\"}",
                        saved.getAfterState()
                )
        );
    }

    @Test
    void listenerUsesAuditGroupAndIsTransactional()
            throws Exception {

        Method method =
                AuditEventConsumer.class.getDeclaredMethod(
                        "onTradeEvent",
                        TradeEvent.class
                );

        KafkaListener listener =
                method.getAnnotation(KafkaListener.class);

        assertNotNull(listener);
        assertArrayEquals(
                new String[]{"trade-events"},
                listener.topics()
        );
        assertEquals(
                "audit-service",
                listener.groupId()
        );

        assertNotNull(
                method.getAnnotation(Transactional.class)
        );
    }
}