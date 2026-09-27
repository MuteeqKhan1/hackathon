package com.learningplatform.source.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.operation.service.OperationService;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import com.learningplatform.source.repository.SourceMaterialRepository;
import com.learningplatform.source.repository.SourceMaterialVersionRepository;
import com.learningplatform.sync.service.SyncPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Parse PDF → sections/chunks/diff → mark PUBLISHED → trigger sync (PRD-04 / PRD-09).
 */
@Component
public class SourcePublishWorker {

    private static final Logger log = LoggerFactory.getLogger(SourcePublishWorker.class);

    private final SourceMaterialVersionRepository versionRepository;
    private final SourceMaterialRepository materialRepository;
    private final OperationService operationService;
    private final AuditService auditService;
    private final DocumentParseService documentParseService;
    private final SyncPipelineService syncPipelineService;

    public SourcePublishWorker(
            SourceMaterialVersionRepository versionRepository,
            SourceMaterialRepository materialRepository,
            OperationService operationService,
            AuditService auditService,
            DocumentParseService documentParseService,
            SyncPipelineService syncPipelineService
    ) {
        this.versionRepository = versionRepository;
        this.materialRepository = materialRepository;
        this.operationService = operationService;
        this.auditService = auditService;
        this.documentParseService = documentParseService;
        this.syncPipelineService = syncPipelineService;
    }

    @Async
    @Transactional
    public void processPublish(UUID operationId, UUID materialId, UUID versionId) {
        try {
            operationService.markRunning(operationId, 15);
            SourceMaterialVersion version = versionRepository.findByIdAndSourceMaterialId(versionId, materialId)
                    .orElseThrow(() -> new NotFoundException("SOURCE_VERSION_NOT_FOUND", "Source version was not found."));

            if (version.getStatus() == SourceVersionStatus.PUBLISHED) {
                operationService.markCompleted(operationId, "{\"status\":\"ALREADY_PUBLISHED\"}");
                return;
            }

            operationService.markRunning(operationId, 45);
            DocumentParseService.ParseResult parseResult = documentParseService.parseAndPersist(version);

            operationService.markRunning(operationId, 85);
            version.markPublished(Instant.now());
            versionRepository.save(version);
            auditService.record(
                    "SOURCE_VERSION_PUBLISHED",
                    "SourceMaterialVersion",
                    versionId.toString(),
                    "material=" + materialId + ",version=" + version.getVersionNumber()
                            + ",sections=" + parseResult.sectionCount()
            );

            UUID syncEventId = triggerSyncIfPreviousExists(materialId, version);

            operationService.markCompleted(
                    operationId,
                    "{\"sourceMaterialId\":\"" + materialId + "\",\"versionId\":\"" + versionId
                            + "\",\"versionNumber\":" + version.getVersionNumber()
                            + ",\"sections\":" + parseResult.sectionCount()
                            + ",\"chunks\":" + parseResult.chunkCount()
                            + ",\"added\":" + parseResult.added()
                            + ",\"modified\":" + parseResult.modified()
                            + (syncEventId != null ? ",\"syncEventId\":\"" + syncEventId + "\"" : "")
                            + "}"
            );
        } catch (Exception ex) {
            log.error("Publish/parse failed for version {}", versionId, ex);
            versionRepository.findById(versionId).ifPresent(v -> {
                if (v.getStatus() != SourceVersionStatus.PUBLISHED) {
                    v.markFailed();
                    versionRepository.save(v);
                }
            });
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            operationService.markFailed(operationId, "SOURCE_PUBLISH_FAILED", message);
        }
    }

    private UUID triggerSyncIfPreviousExists(UUID materialId, SourceMaterialVersion newVersion) {
        List<SourceMaterialVersion> versions =
                versionRepository.findBySourceMaterialIdOrderByVersionNumberAsc(materialId);
        SourceMaterialVersion previous = versions.stream()
                .filter(v -> v.getStatus() == SourceVersionStatus.PUBLISHED)
                .filter(v -> v.getVersionNumber() < newVersion.getVersionNumber())
                .max(Comparator.comparingInt(SourceMaterialVersion::getVersionNumber))
                .orElse(null);
        if (previous == null) {
            return null;
        }
        SourceMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new NotFoundException("SOURCE_MATERIAL_NOT_FOUND", "Source material was not found."));
        try {
            return syncPipelineService
                    .consumeSourceVersionPublished(
                            material.getOrganizationId(),
                            materialId,
                            previous.getId(),
                            newVersion.getId()
                    )
                    .getId();
        } catch (RuntimeException ex) {
            log.error("Sync pipeline failed after publish of {}", newVersion.getId(), ex);
            return null;
        }
    }
}
