package com.learningplatform.sync.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.domain.SyncEventStatus;
import com.learningplatform.sync.repository.SyncEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SyncEventFactory {

    private final SyncEventRepository syncEventRepository;
    private final AuditService auditService;

    public SyncEventFactory(SyncEventRepository syncEventRepository, AuditService auditService) {
        this.syncEventRepository = syncEventRepository;
        this.auditService = auditService;
    }

    public static String idempotencyKey(UUID materialId, UUID oldVersionId, UUID newVersionId) {
        return materialId + ":" + oldVersionId + ":" + newVersionId;
    }

    @Transactional
    public SyncEvent createOrGet(UUID organizationId, UUID materialId, UUID oldVersionId, UUID newVersionId) {
        String key = idempotencyKey(materialId, oldVersionId, newVersionId);
        return syncEventRepository.findByIdempotencyKey(key).orElseGet(() -> {
            SyncEvent created = new SyncEvent(
                    UUID.randomUUID(),
                    organizationId,
                    materialId,
                    oldVersionId,
                    newVersionId,
                    SyncEventStatus.DETECTED,
                    key,
                    Instant.now()
            );
            SyncEvent saved = syncEventRepository.save(created);
            auditService.record(
                    "SOURCE_CHANGE_DETECTED",
                    "SyncEvent",
                    saved.getId().toString(),
                    "material=" + materialId + ",old=" + oldVersionId + ",new=" + newVersionId
            );
            return saved;
        });
    }
}
