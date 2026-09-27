package com.learningplatform.source.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.service.OperationService;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialStatus;
import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import com.learningplatform.source.repository.SourceMaterialVersionRepository;
import com.learningplatform.source.storage.ObjectStorage;
import com.learningplatform.source.validation.PdfUploadValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceVersionServiceTest {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MATERIAL = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private SourceMaterialService sourceMaterialService;
    @Mock
    private SourceMaterialVersionRepository versionRepository;
    @Mock
    private ObjectStorage objectStorage;
    @Mock
    private PdfUploadValidator pdfUploadValidator;
    @Mock
    private AuditService auditService;
    @Mock
    private OperationService operationService;
    @Mock
    private SourcePublishWorker sourcePublishWorker;

    @InjectMocks
    private SourceVersionService sourceVersionService;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG, UserRole.CONTENT_OWNER, Permission.forRole(UserRole.CONTENT_OWNER)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void upload_pdf_createsVersion1() {
        stubMaterial();
        when(versionRepository.findMaxVersionNumber(MATERIAL)).thenReturn(0);
        doNothing().when(pdfUploadValidator).validate(any());
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "p.pdf", "application/pdf", "%PDF-1.4".getBytes());
        SourceMaterialVersion version = sourceVersionService.upload(MATERIAL, file);

        assertThat(version.getVersionNumber()).isEqualTo(1);
        assertThat(version.getStatus()).isEqualTo(SourceVersionStatus.DRAFT);
        verify(objectStorage).put(anyString(), any(), anyLong(), eq("application/pdf"));
    }

    @Test
    void upload_secondPdf_createsVersion2() {
        stubMaterial();
        when(versionRepository.findMaxVersionNumber(MATERIAL)).thenReturn(1);
        doNothing().when(pdfUploadValidator).validate(any());
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "p2.pdf", "application/pdf", "%PDF-1.7".getBytes());
        SourceMaterialVersion version = sourceVersionService.upload(MATERIAL, file);

        assertThat(version.getVersionNumber()).isEqualTo(2);
    }

    @Test
    void publish_alreadyPublished_rejects() {
        stubMaterial();
        UUID versionId = UUID.randomUUID();
        SourceMaterialVersion published = new SourceMaterialVersion(
                versionId, MATERIAL, 1, "k", "a.pdf", "application/pdf", 1, "h",
                SourceVersionStatus.PUBLISHED, USER, Instant.now(), Instant.now()
        );
        when(versionRepository.findByIdAndSourceMaterialId(versionId, MATERIAL)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> sourceVersionService.publish(MATERIAL, versionId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ILLEGAL_STATE");
    }

    @Test
    void publish_draft_schedulesOperation() {
        stubMaterial();
        UUID versionId = UUID.randomUUID();
        SourceMaterialVersion draft = new SourceMaterialVersion(
                versionId, MATERIAL, 1, "k", "a.pdf", "application/pdf", 1, "h",
                SourceVersionStatus.DRAFT, USER, Instant.now(), null
        );
        when(versionRepository.findByIdAndSourceMaterialId(versionId, MATERIAL)).thenReturn(Optional.of(draft));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AsyncOperation op = AsyncOperation.initiated(ORG, "SOURCE_VERSION_PUBLISH", "c1");
        when(operationService.create("SOURCE_VERSION_PUBLISH")).thenReturn(op);

        AsyncOperation result = sourceVersionService.publish(MATERIAL, versionId);

        assertThat(result.getId()).isEqualTo(op.getId());
        ArgumentCaptor<SourceMaterialVersion> captor = ArgumentCaptor.forClass(SourceMaterialVersion.class);
        verify(versionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SourceVersionStatus.PROCESSING);
        verify(sourcePublishWorker).processPublish(op.getId(), MATERIAL, versionId);
    }

    private void stubMaterial() {
        when(sourceMaterialService.get(MATERIAL)).thenReturn(new SourceMaterial(
                MATERIAL, ORG, "Physics", null, "Science", SourceMaterialStatus.ACTIVE, USER, Instant.now()
        ));
    }
}
