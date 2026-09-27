package com.learningplatform.content.lineage;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.domain.SyncPolicy;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.repository.ContentSourceMappingRepository;
import com.learningplatform.source.domain.SourceSection;
import com.learningplatform.source.repository.SourceSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LineageQueryService {

    private final ContentAssetRepository assetRepository;
    private final ContentAssetVersionRepository versionRepository;
    private final ContentSourceMappingRepository mappingRepository;
    private final SourceSectionRepository sourceSectionRepository;

    public LineageQueryService(
            ContentAssetRepository assetRepository,
            ContentAssetVersionRepository versionRepository,
            ContentSourceMappingRepository mappingRepository,
            SourceSectionRepository sourceSectionRepository
    ) {
        this.assetRepository = assetRepository;
        this.versionRepository = versionRepository;
        this.mappingRepository = mappingRepository;
        this.sourceSectionRepository = sourceSectionRepository;
    }

    @Transactional(readOnly = true)
    public List<AssetSourceView> sourcesForAsset(UUID assetId) {
        PermissionGuard.require(Permission.CONTENT_EDIT);
        ContentAsset asset = requireAssetInOrg(assetId);
        List<ContentSourceMapping> mappings = mappingRepository.findByContentAssetId(asset.getId());
        // Prefer current version mappings when present
        if (asset.getCurrentVersionId() != null) {
            List<ContentSourceMapping> current = mappingRepository.findByContentAssetVersionId(asset.getCurrentVersionId());
            if (!current.isEmpty()) {
                mappings = current;
            }
        }
        List<AssetSourceView> views = new ArrayList<>();
        for (ContentSourceMapping m : mappings) {
            SourceSection section = sourceSectionRepository.findById(m.getSourceSectionId()).orElse(null);
            views.add(new AssetSourceView(
                    m.getSourceSectionId(),
                    m.getSourceSectionVersionId(),
                    section != null ? section.getExternalReference() : null,
                    section != null ? section.getTitle() : null,
                    m.getRelationshipType()
            ));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<SectionDependentView> dependentsForSection(UUID sourceSectionId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.CONTENT_EDIT);
        if (!sourceSectionRepository.existsById(sourceSectionId)) {
            throw new NotFoundException("SOURCE_SECTION_NOT_FOUND", "Source section was not found.");
        }
        List<ContentSourceMapping> mappings = mappingRepository.findBySourceSectionId(sourceSectionId);
        Map<UUID, SectionDependentView> byAsset = new LinkedHashMap<>();
        for (ContentSourceMapping m : mappings) {
            ContentAsset asset = assetRepository.findById(m.getContentAssetId()).orElse(null);
            if (asset == null || !asset.getOrganizationId().equals(user.organizationId())) {
                continue;
            }
            byAsset.putIfAbsent(asset.getId(), new SectionDependentView(
                    asset.getId(),
                    asset.getCourseId(),
                    asset.getTopicId(),
                    asset.getAssetType().name(),
                    asset.getStatus().name(),
                    m.getRelationshipType(),
                    currentSyncPolicy(asset)
            ));
        }
        return List.copyOf(byAsset.values());
    }

    private SyncPolicy currentSyncPolicy(ContentAsset asset) {
        if (asset.getCurrentVersionId() == null) {
            return SyncPolicy.AUTO;
        }
        return versionRepository.findById(asset.getCurrentVersionId())
                .map(ContentAssetVersion::getSyncPolicyEnum)
                .orElse(SyncPolicy.AUTO);
    }

    private ContentAsset requireAssetInOrg(UUID assetId) {
        ContentAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("CONTENT_ASSET_NOT_FOUND", "Content asset was not found."));
        PermissionGuard.requireSameOrganization(asset.getOrganizationId());
        return asset;
    }

    public record AssetSourceView(
            UUID sourceSectionId,
            UUID sourceSectionVersionId,
            String externalReference,
            String title,
            MappingRelationshipType relationshipType
    ) {
    }

    public record SectionDependentView(
            UUID contentAssetId,
            UUID courseId,
            UUID topicId,
            String assetType,
            String status,
            MappingRelationshipType relationshipType,
            SyncPolicy syncPolicy
    ) {
    }
}
