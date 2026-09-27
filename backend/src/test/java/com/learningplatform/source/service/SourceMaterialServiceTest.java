package com.learningplatform.source.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialStatus;
import com.learningplatform.source.repository.SourceMaterialRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceMaterialServiceTest {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private SourceMaterialRepository sourceMaterialRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private SourceMaterialService sourceMaterialService;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG, UserRole.CONTENT_OWNER, Permission.forRole(UserRole.CONTENT_OWNER)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void create_sameOrg_persistsActiveMaterial() {
        when(sourceMaterialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SourceMaterial material = sourceMaterialService.create("Physics Grade 10", "desc", "Physics");

        assertThat(material.getOrganizationId()).isEqualTo(ORG);
        assertThat(material.getStatus()).isEqualTo(SourceMaterialStatus.ACTIVE);
        assertThat(material.getTitle()).isEqualTo("Physics Grade 10");
    }
}
