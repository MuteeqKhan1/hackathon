package com.learningplatform.audit.service;

import com.learningplatform.audit.domain.AuditEvent;
import com.learningplatform.audit.repository.AuditEventRepository;
import com.learningplatform.common.correlation.CorrelationIdFilter;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(String action, String entityType, String entityId, String newValue) {
        AuthenticatedUser actor = SecurityContext.get();
        UUID orgId = actor != null ? actor.organizationId() : null;
        UUID userId = actor != null ? actor.userId() : null;
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);

        auditEventRepository.save(new AuditEvent(
                UUID.randomUUID(),
                orgId,
                userId,
                action,
                entityType,
                entityId,
                correlationId,
                null,
                newValue,
                Instant.now()
        ));
    }
}
