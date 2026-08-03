package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.model.DlqMessage;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repo;

    public DlqConsumer(DlqMessageRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "trade-events-dlq", groupId = "dlq-monitor")
    public void onDlqMessage(ConsumerRecord<String, TradeEvent> record,
                             @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exMsg) {
        TradeEvent event = record.value();

        if (event == null) {
            log.error("DLQ message received with null payload for topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        DlqMessage dlqMessage = new DlqMessage(
                event.eventId(),
                event.tradeRef(),
                record.topic().replace("-dlq", ""),
                record.partition(),
                record.offset(),
                event,
                exMsg,
                Instant.now()
        );

        repo.save(dlqMessage);

        log.error("DLQ persisted: eventId={} tradeRef={} originalTopic={} partition={} offset={} reason={}",
                event.eventId(),
                event.tradeRef(),
                dlqMessage.getOriginalTopic(),
                record.partition(),
                record.offset(),
                exMsg);
    }
}
