package com.learningplatform.sync.api;

import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.SyncAction;
import com.learningplatform.sync.domain.SyncActionType;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.service.SyncActionService;
import com.learningplatform.sync.service.SyncQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync-events")
public class SyncController {

    private final SyncQueryService syncQueryService;
    private final SyncActionService syncActionService;

    public SyncController(SyncQueryService syncQueryService, SyncActionService syncActionService) {
        this.syncQueryService = syncQueryService;
        this.syncActionService = syncActionService;
    }

    @GetMapping
    public List<SyncEventView> list() {
        return syncQueryService.listEvents().stream().map(SyncEventView::from).toList();
    }

    @GetMapping("/{id}")
    public SyncEventView get(@PathVariable UUID id) {
        return SyncEventView.from(syncQueryService.getEvent(id));
    }

    @GetMapping("/{id}/impact")
    public List<ImpactView> impact(@PathVariable UUID id) {
        return syncQueryService.listImpacts(id).stream().map(ImpactView::from).toList();
    }

    @PostMapping("/{id}/actions")
    public Object actions(@PathVariable UUID id, @Valid @RequestBody ActionBody body) {
        if (body.actions() != null && !body.actions().isEmpty()) {
            List<SyncAction> results = syncActionService.applyBulk(
                    id,
                    body.actions().stream()
                            .map(a -> new SyncActionService.ActionRequest(a.assetId(), a.action()))
                            .toList()
            );
            return results.stream().map(ActionResultView::from).toList();
        }
        SyncAction result = syncActionService.apply(id, body.assetId(), body.action());
        return ActionResultView.from(result);
    }

    public record ActionBody(
            UUID assetId,
            SyncActionType action,
            List<SingleAction> actions
    ) {
    }

    public record SingleAction(@NotNull UUID assetId, @NotNull SyncActionType action) {
    }

    public record SyncEventView(
            UUID id,
            UUID sourceMaterialId,
            UUID oldVersionId,
            UUID newVersionId,
            String status,
            String idempotencyKey,
            String createdAt
    ) {
        static SyncEventView from(SyncEvent e) {
            return new SyncEventView(
                    e.getId(),
                    e.getSourceMaterialId(),
                    e.getOldVersionId(),
                    e.getNewVersionId(),
                    e.getStatus().name(),
                    e.getIdempotencyKey(),
                    e.getCreatedAt().toString()
            );
        }
    }

    public record ImpactView(
            UUID id,
            UUID courseId,
            UUID contentAssetId,
            UUID sourceSectionId,
            String impactLevel,
            String reason,
            String recommendedAction,
            String status,
            String changeSignature
    ) {
        static ImpactView from(ImpactAnalysis i) {
            return new ImpactView(
                    i.getId(),
                    i.getCourseId(),
                    i.getContentAssetId(),
                    i.getSourceSectionId(),
                    i.getImpactLevel().name(),
                    i.getReason(),
                    i.getRecommendedAction().name(),
                    i.getStatus().name(),
                    i.getChangeSignature()
            );
        }
    }

    public record ActionResultView(
            UUID id,
            UUID impactAnalysisId,
            String action,
            String status,
            UUID oldAssetVersionId,
            UUID newAssetVersionId,
            String detail
    ) {
        static ActionResultView from(SyncAction a) {
            return new ActionResultView(
                    a.getId(),
                    a.getImpactAnalysisId(),
                    a.getAction().name(),
                    a.getStatus().name(),
                    a.getOldAssetVersionId(),
                    a.getNewAssetVersionId(),
                    a.getDetail()
            );
        }
    }
}
