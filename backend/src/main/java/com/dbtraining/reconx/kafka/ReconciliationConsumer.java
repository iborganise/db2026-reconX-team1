package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.ReconciliationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(ReconciliationConsumer.class);

    private final ReconciliationEngine reconEngine;


    public ReconciliationConsumer(ReconciliationEngine reconEngine) {
        this.reconEngine = reconEngine;
    }


    @KafkaListener(
            topics = "trade-events",
            groupId = "recon-service"
    )
    public void onTradeEvent(TradeEvent event) {

        log.info(
                "ReconciliationConsumer received TradeEvent eventId={} ref={} type={}",
                event.eventId(),
                event.tradeRef(),
                event.eventType()
        );


        switch (event.eventType()) {

            case TRADE_CREATED:
            case TRADE_UPDATED:

                reconEngine.scheduleRecon(event.tradeRef());
                break;


            case TRADE_CANCELLED:

                reconEngine.cancelPendingRecon(event.tradeRef());
                break;


            default:

                log.warn(
                        "Unknown TradeEvent type: {}",
                        event.eventType()
                );
        }
    }
}