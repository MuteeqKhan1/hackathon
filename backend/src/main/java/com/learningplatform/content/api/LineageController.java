package com.learningplatform.content.api;

import com.learningplatform.content.domain.SyncPolicy;
import com.learningplatform.content.lineage.LineageQueryService;
import com.learningplatform.content.lineage.SyncPolicyService;
import com.learningplatform.content.domain.ContentAssetVersion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LineageController {

    private final LineageQueryService lineageQueryService;
    private final SyncPolicyService syncPolicyService;

    public LineageController(LineageQueryService lineageQueryService, SyncPolicyService syncPolicyService) {
        this.lineageQueryService = lineageQueryService;
        this.syncPolicyService = syncPolicyService;
    }

    @GetMapping("/content-assets/{assetId}/sources")
    public List<LineageQueryService.AssetSourceView> sources(@PathVariable UUID assetId) {
        return lineageQueryService.sourcesForAsset(assetId);
    }

    @GetMapping("/source-sections/{sectionId}/dependents")
    public List<LineageQueryService.SectionDependentView> dependents(@PathVariable UUID sectionId) {
        return lineageQueryService.dependentsForSection(sectionId);
    }

    @PutMapping("/content-assets/{assetId}/sync-policy")
    public SyncPolicyResponse setSyncPolicy(
            @PathVariable UUID assetId,
            @Valid @RequestBody SyncPolicyRequest request
    ) {
        ContentAssetVersion version = syncPolicyService.setPolicy(assetId, request.policy());
        return new SyncPolicyResponse(assetId, version.getId(), version.getSyncPolicyEnum());
    }

    public record SyncPolicyRequest(@NotNull SyncPolicy policy) {
    }

    public record SyncPolicyResponse(UUID assetId, UUID versionId, SyncPolicy policy) {
    }
}
