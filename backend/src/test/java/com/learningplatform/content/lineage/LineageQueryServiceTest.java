package com.learningplatform.content.lineage;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentSourceMapping;
import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.repository.ContentSourceMappingRepository;
import com.learningplatform.source.domain.SourceSection;
import com.learningplatform.source.repository.SourceSectionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LineageQueryServiceTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID SECTION = UUID.randomUUID();

    @Mock private ContentAssetRepository assetRepository;
    @Mock private ContentAssetVersionRepository versionRepository;
    @Mock private ContentSourceMappingRepository mappingRepository;
    @Mock private SourceSectionRepository sourceSectionRepository;

    @InjectMocks
    private LineageQueryService lineageQueryService;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG, UserRole.INSTRUCTOR, Permission.forRole(UserRole.INSTRUCTOR)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void bySection_threeAssets_returnsThree() {
        when(sourceSectionRepository.existsById(SECTION)).thenReturn(true);
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();
        UUID a3 = UUID.randomUUID();
        when(mappingRepository.findBySourceSectionId(SECTION)).thenReturn(List.of(
                mapping(a1), mapping(a2), mapping(a3)
        ));
        when(assetRepository.findById(a1)).thenReturn(Optional.of(asset(a1)));
        when(assetRepository.findById(a2)).thenReturn(Optional.of(asset(a2)));
        when(assetRepository.findById(a3)).thenReturn(Optional.of(asset(a3)));

        assertThat(lineageQueryService.dependentsForSection(SECTION)).hasSize(3);
    }

    @Test
    void byAsset_twoSections_returnsTwo() {
        UUID assetId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ContentAsset asset = asset(assetId);
        asset.setCurrentVersion(versionId, ContentAssetStatus.APPROVED);
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        when(mappingRepository.findByContentAssetId(assetId)).thenReturn(List.of());
        when(mappingRepository.findByContentAssetVersionId(versionId)).thenReturn(List.of(
                new ContentSourceMapping(UUID.randomUUID(), assetId, versionId, s1, UUID.randomUUID(), MappingRelationshipType.PRIMARY),
                new ContentSourceMapping(UUID.randomUUID(), assetId, versionId, s2, UUID.randomUUID(), MappingRelationshipType.SUPPORTING)
        ));
        when(sourceSectionRepository.findById(s1)).thenReturn(Optional.of(
                new SourceSection(s1, UUID.randomUUID(), null, "SEC-1", "One", "SECTION")));
        when(sourceSectionRepository.findById(s2)).thenReturn(Optional.of(
                new SourceSection(s2, UUID.randomUUID(), null, "SEC-2", "Two", "SECTION")));

        assertThat(lineageQueryService.sourcesForAsset(assetId)).hasSize(2);
    }

    private ContentSourceMapping mapping(UUID assetId) {
        return new ContentSourceMapping(
                UUID.randomUUID(), assetId, UUID.randomUUID(), SECTION, UUID.randomUUID(), MappingRelationshipType.PRIMARY
        );
    }

    private ContentAsset asset(UUID id) {
        return new ContentAsset(
                id, ORG, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AssetType.EXPLANATION, ContentAssetStatus.APPROVED, USER, Instant.now(), Instant.now()
        );
    }
}
