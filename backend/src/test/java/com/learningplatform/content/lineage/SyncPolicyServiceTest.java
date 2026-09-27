package com.learningplatform.content.lineage;

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
import com.learningplatform.content.domain.SyncPolicy;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncPolicyServiceTest {

    @Mock private ContentAssetRepository assetRepository;
    @Mock private ContentAssetVersionRepository versionRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private SyncPolicyService syncPolicyService;

    private final UUID org = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(user, org, UserRole.INSTRUCTOR, Permission.forRole(UserRole.INSTRUCTOR)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void setManual_persistsPolicy() {
        ContentAsset asset = new ContentAsset(
                assetId, org, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AssetType.EXPLANATION, ContentAssetStatus.APPROVED, user, Instant.now(), Instant.now()
        );
        asset.setCurrentVersion(versionId, ContentAssetStatus.APPROVED);
        ContentAssetVersion version = new ContentAssetVersion(
                versionId, assetId, 1, "{}", "en", UUID.randomUUID(),
                GenerationMethod.AI, "1.0.0", "heuristic", "1.0.0",
                ContentAssetStatus.APPROVED, false, SyncPolicy.AUTO.name(), user, Instant.now()
        );
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContentAssetVersion updated = syncPolicyService.setPolicy(assetId, SyncPolicy.MANUAL);

        assertThat(updated.getSyncPolicyEnum()).isEqualTo(SyncPolicy.MANUAL);
    }
}
