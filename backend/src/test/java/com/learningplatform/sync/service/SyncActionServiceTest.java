package com.learningplatform.sync.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.domain.ContentAssetVersion;
import com.learningplatform.content.domain.GenerationMethod;
import com.learningplatform.content.domain.SyncPolicy;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.service.GenerationService;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactLevel;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncActionServiceTest {

    @Mock private SyncEventRepository syncEventRepository;
    @Mock private ImpactAnalysisRepository impactAnalysisRepository;
    @Mock private SyncActionRepository syncActionRepository;
    @Mock private ContentAssetRepository assetRepository;
    @Mock private ContentAssetVersionRepository versionRepository;
    @Mock private GenerationService generationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private SyncActionService service;

    private final UUID org = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID newVersionId = UUID.randomUUID();

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(user, org, UserRole.INSTRUCTOR, Permission.forRole(UserRole.INSTRUCTOR)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void regenerateSelected_createsDraftAndCompletes() {
        SyncEvent event = event(SyncEventStatus.NOTIFIED);
        ImpactAnalysis impact = openImpact(RecommendedAction.REGENERATE);
        ContentAsset asset = asset(false);
        ContentAssetVersion newDraft = draftVersion();

        when(syncEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(impactAnalysisRepository.findBySyncEventId(eventId)).thenReturn(List.of(impact));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(syncActionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(generationService.regenerate(eq(assetId), eq(user), eq(newVersionId)))
                .thenReturn(new GenerationService.GeneratedAssetResult(asset, newDraft, 1));
        when(impactAnalysisRepository.findBySyncEventId(eventId)).thenReturn(List.of(impact));

        SyncAction action = service.apply(eventId, assetId, SyncActionType.REGENERATE);

        assertThat(action.getStatus()).isEqualTo(SyncActionStatus.COMPLETED);
        assertThat(action.getNewAssetVersionId()).isEqualTo(newDraft.getId());
        assertThat(impact.getStatus()).isEqualTo(ImpactStatus.CLOSED);
        verify(generationService).regenerate(assetId, user, newVersionId);
    }

    @Test
    void ignore_closesImpact_noRegen() {
        SyncEvent event = event(SyncEventStatus.NOTIFIED);
        ImpactAnalysis impact = openImpact(RecommendedAction.REGENERATE);
        ContentAsset asset = asset(false);

        when(syncEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(impactAnalysisRepository.findBySyncEventId(eventId)).thenReturn(List.of(impact));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(syncActionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncAction action = service.apply(eventId, assetId, SyncActionType.IGNORE);

        assertThat(action.getStatus()).isEqualTo(SyncActionStatus.COMPLETED);
        assertThat(impact.getStatus()).isEqualTo(ImpactStatus.CLOSED);
        verify(generationService, never()).regenerate(any(), any(), any());
    }

    @Test
    void manualModification_reviewConflict_noOverwrite() {
        SyncEvent event = event(SyncEventStatus.NOTIFIED);
        ImpactAnalysis impact = openImpact(RecommendedAction.REVIEW_CONFLICT);
        ContentAsset asset = asset(true);

        when(syncEventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(impactAnalysisRepository.findBySyncEventId(eventId)).thenReturn(List.of(impact));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(syncActionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.apply(eventId, assetId, SyncActionType.REGENERATE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("manual");
        verify(generationService, never()).regenerate(any(), any(), any());
    }

    private SyncEvent event(SyncEventStatus status) {
        return new SyncEvent(
                eventId, org, UUID.randomUUID(), UUID.randomUUID(), newVersionId, status, "key", Instant.now()
        );
    }

    private ImpactAnalysis openImpact(RecommendedAction recommended) {
        return new ImpactAnalysis(
                UUID.randomUUID(), eventId, UUID.randomUUID(), assetId, UUID.randomUUID(),
                ImpactLevel.HIGH, "reason", recommended, "sig", ImpactStatus.OPEN, Instant.now()
        );
    }

    private ContentAsset asset(boolean manual) {
        ContentAsset a = new ContentAsset(
                assetId, org, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AssetType.QUIZ, ContentAssetStatus.APPROVED, user, Instant.now(), Instant.now()
        );
        a.setCurrentVersion(versionId, ContentAssetStatus.APPROVED);
        return a;
    }

    private ContentAssetVersion draftVersion() {
        return new ContentAssetVersion(
                UUID.randomUUID(), assetId, 2, "{}", "en", newVersionId,
                GenerationMethod.AI, "1", "h", "1", ContentAssetStatus.PENDING_REVIEW,
                false, SyncPolicy.AUTO.name(), user, Instant.now()
        );
    }
}
