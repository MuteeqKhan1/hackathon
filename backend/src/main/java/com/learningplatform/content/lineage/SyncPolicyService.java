package com.learningplatform.content.lineage;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.SyncPolicy;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SyncPolicyService {

    private final ContentAssetRepository assetRepository;
    private final ContentAssetVersionRepository versionRepository;
    private final AuditService auditService;

    public SyncPolicyService(
            ContentAssetRepository assetRepository,
            ContentAssetVersionRepository versionRepository,
            AuditService auditService
    ) {
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ContentAssetVersion setPolicy(UUID assetId, SyncPolicy policy) {
        PermissionGuard.require(Permission.CONTENT_EDIT);
        if (policy == null) {
            throw new IllegalArgumentException("policy must be AUTO or MANUAL");
        }
        ContentAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("CONTENT_ASSET_NOT_FOUND", "Content asset was not found."));
        PermissionGuard.requireSameOrganization(asset.getOrganizationId());
        if (asset.getCurrentVersionId() == null) {
            throw new NotFoundException("CONTENT_VERSION_NOT_FOUND", "Asset has no current version.");
        }
        ContentAssetVersion version = versionRepository.findById(asset.getCurrentVersionId())
                .orElseThrow(() -> new NotFoundException("CONTENT_VERSION_NOT_FOUND", "Asset version was not found."));
        version.setSyncPolicy(policy);
        versionRepository.save(version);
        auditService.record("CONTENT_SYNC_POLICY_SET", "ContentAssetVersion", version.getId().toString(), policy.name());
        return version;
    }
}
