package com.learningplatform.content.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.GenerationMethod;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentReviewServiceTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID ASSET_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();

    @Mock
    private ContentAssetRepository assetRepository;
    @Mock
    private ContentAssetVersionRepository versionRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ContentReviewService contentReviewService;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG, UserRole.INSTRUCTOR, Permission.forRole(UserRole.INSTRUCTOR)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void edit_setsManualModification() {
        ContentAsset asset = asset(ContentAssetStatus.PENDING_REVIEW);
        ContentAssetVersion version = version(ContentAssetStatus.PENDING_REVIEW, false);
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContentAssetVersion edited = contentReviewService.edit(ASSET_ID, "{\"title\":\"Edited\"}");

        assertThat(edited.isManualModification()).isTrue();
        assertThat(edited.getContentJson()).contains("Edited");
        assertThat(edited.getStatus()).isEqualTo(ContentAssetStatus.PENDING_REVIEW);
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("CONTENT_EDITED"), any(), any(), any());
    }

    @Test
    void approve_pendingReview_setsApprovedBy() {
        ContentAsset asset = asset(ContentAssetStatus.PENDING_REVIEW);
        ContentAssetVersion version = version(ContentAssetStatus.PENDING_REVIEW, false);
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContentAssetVersion approved = contentReviewService.approve(ASSET_ID);

        assertThat(approved.getStatus()).isEqualTo(ContentAssetStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo(USER);
        assertThat(approved.getApprovedAt()).isNotNull();
        verify(auditService).record(org.mockito.ArgumentMatchers.eq("CONTENT_APPROVED"), any(), any(), any());
    }

    @Test
    void reject_pendingReview_setsRejected() {
        ContentAsset asset = asset(ContentAssetStatus.PENDING_REVIEW);
        ContentAssetVersion version = version(ContentAssetStatus.PENDING_REVIEW, false);
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(version));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContentAssetVersion rejected = contentReviewService.reject(ASSET_ID);

        assertThat(rejected.getStatus()).isEqualTo(ContentAssetStatus.REJECTED);
    }

    private ContentAsset asset(ContentAssetStatus status) {
        ContentAsset asset = new ContentAsset(
                ASSET_ID, ORG, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AssetType.EXPLANATION, status, USER, Instant.now(), Instant.now()
        );
        asset.setCurrentVersion(VERSION_ID, status);
        return asset;
    }

    private ContentAssetVersion version(ContentAssetStatus status, boolean manual) {
        return new ContentAssetVersion(
                VERSION_ID, ASSET_ID, 1, "{}", "en", UUID.randomUUID(),
                GenerationMethod.AI, "1.0.0", "heuristic-rag", "1.0.0",
                status, manual, "FOLLOW_SOURCE", USER, Instant.now()
        );
    }
}
