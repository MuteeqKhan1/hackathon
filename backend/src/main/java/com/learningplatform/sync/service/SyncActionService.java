package com.learningplatform.sync.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.service.GenerationService;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactStatus;
import com.learningplatform.sync.domain.RecommendedAction;
import com.learningplatform.sync.domain.SyncAction;
import com.learningplatform.sync.domain.SyncActionStatus;
import com.learningplatform.sync.domain.SyncActionType;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.domain.SyncEventStatus;
import com.learningplatform.sync.repository.ImpactAnalysisRepository;
import com.learningplatform.sync.repository.SyncActionRepository;
import com.learningplatform.sync.repository.SyncEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SyncActionService {

    private final SyncEventRepository syncEventRepository;
    private final ImpactAnalysisRepository impactAnalysisRepository;
    private final SyncActionRepository syncActionRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAssetVersionRepository versionRepository;
    private final GenerationService generationService;
    private final AuditService auditService;

    public SyncActionService(
            SyncEventRepository syncEventRepository,
            ImpactAnalysisRepository impactAnalysisRepository,
            SyncActionRepository syncActionRepository,
            ContentAssetRepository assetRepository,
            ContentAssetVersionRepository versionRepository,
            GenerationService generationService,
            AuditService auditService
    ) {
        this.syncEventRepository = syncEventRepository;
        this.impactAnalysisRepository = impactAnalysisRepository;
        this.syncActionRepository = syncActionRepository;
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.generationService = generationService;
        this.auditService = auditService;
    }

    @Transactional
    public SyncAction apply(UUID syncEventId, UUID assetId, SyncActionType action) {
        AuthenticatedUser user = PermissionGuard.require(Permission.SYNC_REVIEW);
        SyncEvent event = requireEventInOrg(syncEventId, user.organizationId());
        List<ImpactAnalysis> openImpacts = impactAnalysisRepository.findBySyncEventId(syncEventId).stream()
                .filter(i -> i.getContentAssetId().equals(assetId) && i.getStatus() == ImpactStatus.OPEN)
                .toList();
        if (openImpacts.isEmpty()) {
            throw new NotFoundException("IMPACT_NOT_FOUND", "No open impact for asset in this sync event.");
        }
        ImpactAnalysis impact = openImpacts.get(0);

        event.transitionTo(SyncEventStatus.IN_PROGRESS);
        syncEventRepository.save(event);

        ContentAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("CONTENT_ASSET_NOT_FOUND", "Content asset was not found."));
        UUID oldVersionId = asset.getCurrentVersionId();

        SyncAction syncAction = new SyncAction(
                UUID.randomUUID(),
                impact.getId(),
                action,
                user.userId(),
                SyncActionStatus.IN_PROGRESS,
                oldVersionId,
                Instant.now()
        );
        syncActionRepository.save(syncAction);
        auditService.record("SYNC_ACTION_REQUESTED", "SyncAction", syncAction.getId().toString(), action.name());

        try {
            if (action == SyncActionType.IGNORE || action == SyncActionType.REJECT) {
                closeAll(openImpacts);
                syncAction.complete(null, action.name() + " — impact closed");
                syncActionRepository.save(syncAction);
                auditService.record("SYNC_ACTION_COMPLETED", "SyncAction", syncAction.getId().toString(), action.name());
                maybeCompleteEvent(event);
                return syncAction;
            }

            if (action == SyncActionType.ACCEPT) {
                closeAll(openImpacts);
                syncAction.complete(oldVersionId, "Accepted current content");
                syncActionRepository.save(syncAction);
                auditService.record("SYNC_ACTION_COMPLETED", "SyncAction", syncAction.getId().toString(), "ACCEPT");
                maybeCompleteEvent(event);
                return syncAction;
            }

            // REGENERATE
            if (impact.getRecommendedAction() == RecommendedAction.REVIEW_CONFLICT
                    || isManual(asset)) {
                syncAction.fail("REVIEW_CONFLICT — no overwrite of manual_modification");
                syncActionRepository.save(syncAction);
                auditService.record("SYNC_ACTION_FAILED", "SyncAction", syncAction.getId().toString(), "REVIEW_CONFLICT");
                throw new BusinessException("REVIEW_CONFLICT", "Asset has manual modifications; regenerate blocked.");
            }

            GenerationService.GeneratedAssetResult result =
                    generationService.regenerate(assetId, user.userId(), event.getNewVersionId());
            closeAll(openImpacts);
            syncAction.complete(result.version().getId(), "Regenerated draft version");
            syncActionRepository.save(syncAction);
            auditService.record(
                    "CONTENT_REGENERATED",
                    "ContentAsset",
                    assetId.toString(),
                    "version=" + result.version().getId()
            );
            auditService.record("SYNC_ACTION_COMPLETED", "SyncAction", syncAction.getId().toString(), "REGENERATE");
            maybeCompleteEvent(event);
            return syncAction;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            syncAction.fail(ex.getMessage());
            syncActionRepository.save(syncAction);
            throw ex;
        }
    }

    private void closeAll(List<ImpactAnalysis> openImpacts) {
        for (ImpactAnalysis open : openImpacts) {
            open.close();
            impactAnalysisRepository.save(open);
        }
    }

    @Transactional
    public List<SyncAction> applyBulk(UUID syncEventId, List<ActionRequest> requests) {
        List<SyncAction> results = new ArrayList<>();
        for (ActionRequest req : requests) {
            results.add(apply(syncEventId, req.assetId(), req.action()));
        }
        return results;
    }

    private boolean isManual(ContentAsset asset) {
        if (asset.getCurrentVersionId() == null) {
            return false;
        }
        return versionRepository.findById(asset.getCurrentVersionId())
                .map(ContentAssetVersion::isManualModification)
                .orElse(false);
    }

    private void maybeCompleteEvent(SyncEvent event) {
        boolean anyOpen = impactAnalysisRepository.findBySyncEventId(event.getId()).stream()
                .anyMatch(i -> i.getStatus() == ImpactStatus.OPEN);
        if (!anyOpen) {
            event.transitionTo(SyncEventStatus.COMPLETED);
            syncEventRepository.save(event);
            auditService.record("SYNC_COMPLETED", "SyncEvent", event.getId().toString(), null);
        }
    }

    private SyncEvent requireEventInOrg(UUID syncEventId, UUID orgId) {
        SyncEvent event = syncEventRepository.findById(syncEventId)
                .orElseThrow(() -> new NotFoundException("SYNC_EVENT_NOT_FOUND", "Sync event was not found."));
        if (!event.getOrganizationId().equals(orgId)) {
            throw new NotFoundException("SYNC_EVENT_NOT_FOUND", "Sync event was not found.");
        }
        return event;
    }

    public record ActionRequest(UUID assetId, SyncActionType action) {
    }
}
