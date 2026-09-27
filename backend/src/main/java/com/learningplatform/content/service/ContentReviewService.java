package com.learningplatform.content.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ContentReviewService {

    private final ContentAssetRepository assetRepository;
    private final ContentAssetVersionRepository versionRepository;
    private final AuditService auditService;

    public ContentReviewService(
            ContentAssetRepository assetRepository,
            ContentAssetVersionRepository versionRepository,
            AuditService auditService
    ) {
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ContentAssetVersion edit(UUID assetId, String contentJson) {
        AuthenticatedUser user = PermissionGuard.require(Permission.CONTENT_EDIT);
        ContentAsset asset = requireAsset(assetId);
        ContentAssetVersion version = requireCurrentVersion(asset);
        version.applyManualEdit(contentJson);
        versionRepository.save(version);
        asset.setCurrentVersion(version.getId(), ContentAssetStatus.PENDING_REVIEW);
        assetRepository.save(asset);
        auditService.record("CONTENT_EDITED", "ContentAssetVersion", version.getId().toString(), "manual_modification=true");
        return version;
    }

    @Transactional
    public ContentAssetVersion approve(UUID assetId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.CONTENT_EDIT);
        ContentAsset asset = requireAsset(assetId);
        ContentAssetVersion version = requireCurrentVersion(asset);
        version.approve(user.userId(), Instant.now());
        versionRepository.save(version);
        asset.setCurrentVersion(version.getId(), ContentAssetStatus.APPROVED);
        assetRepository.save(asset);
        auditService.record(
                "CONTENT_APPROVED",
                "ContentAssetVersion",
                version.getId().toString(),
                "approved_by=" + user.userId()
        );
        return version;
    }

    @Transactional
    public ContentAssetVersion reject(UUID assetId) {
        PermissionGuard.require(Permission.CONTENT_EDIT);
        ContentAsset asset = requireAsset(assetId);
        ContentAssetVersion version = requireCurrentVersion(asset);
        version.reject();
        versionRepository.save(version);
        asset.setCurrentVersion(version.getId(), ContentAssetStatus.REJECTED);
        assetRepository.save(asset);
        auditService.record("CONTENT_REJECTED", "ContentAssetVersion", version.getId().toString(), "REJECTED");
        return version;
    }

    private ContentAsset requireAsset(UUID assetId) {
        ContentAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("CONTENT_ASSET_NOT_FOUND", "Content asset was not found."));
        PermissionGuard.requireSameOrganization(asset.getOrganizationId());
        return asset;
    }

    private ContentAssetVersion requireCurrentVersion(ContentAsset asset) {
        if (asset.getCurrentVersionId() == null) {
            throw new NotFoundException("CONTENT_VERSION_NOT_FOUND", "Asset has no current version.");
        }
        return versionRepository.findById(asset.getCurrentVersionId())
                .orElseThrow(() -> new NotFoundException("CONTENT_VERSION_NOT_FOUND", "Asset version was not found."));
    }
}
