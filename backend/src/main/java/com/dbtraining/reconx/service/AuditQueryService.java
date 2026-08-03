package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditQueryService {


    private final AuditLogRepository auditLogRepository;


    public AuditQueryService(
            AuditLogRepository auditLogRepository
    ) {
        this.auditLogRepository = auditLogRepository;
    }


    public List<AuditLogEntry> eventsForTrade(String tradeRef) {

        return auditLogRepository
                .findByTradeRefOrderByEventTimestampAsc(tradeRef);
    }
}