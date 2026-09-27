package com.learningplatform.common.organization.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.exception.ConflictException;
import com.learningplatform.common.organization.domain.Organization;
import com.learningplatform.common.organization.domain.OrganizationMembership;
import com.learningplatform.common.organization.domain.OrganizationStatus;
import com.learningplatform.common.organization.domain.PlatformUser;
import com.learningplatform.common.organization.repository.OrganizationMembershipRepository;
import com.learningplatform.common.organization.repository.OrganizationRepository;
import com.learningplatform.common.organization.repository.PlatformUserRepository;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PlatformUserRepository platformUserRepository;
    @Mock
    private OrganizationMembershipRepository membershipRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private OrganizationService organizationService;

    @BeforeEach
    void setSecurity() {
        SecurityContext.set(new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                UserRole.CONTENT_OWNER,
                Permission.forRole(UserRole.CONTENT_OWNER)
        ));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void create_validName_persistsActiveOrganization() {
        when(organizationRepository.existsBySlug(any())).thenReturn(false);
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(platformUserRepository.findById(USER_ID)).thenReturn(Optional.of(
                new PlatformUser(USER_ID, "Owner", "owner@example.com", Instant.now())
        ));
        when(membershipRepository.existsByOrganizationIdAndUserId(any(), any())).thenReturn(false);
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Organization created = organizationService.create("Acme Learning");

        assertThat(created.getName()).isEqualTo("Acme Learning");
        assertThat(created.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(created.getSlug()).isEqualTo("acme-learning");
        verify(auditService).record(any(), any(), any(), any());
    }

    @Test
    void create_blankName_throwsValidationError() {
        assertThatThrownBy(() -> organizationService.create("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void addUser_validRole_createsMembership() {
        Organization org = new Organization(ORG_ID, "Acme", "acme", OrganizationStatus.ACTIVE, Instant.now());
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        UUID memberId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        when(platformUserRepository.findById(memberId)).thenReturn(Optional.of(
                new PlatformUser(memberId, "Instructor", "inst@example.com", Instant.now())
        ));
        when(membershipRepository.existsByOrganizationIdAndUserId(ORG_ID, memberId)).thenReturn(false);
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMembership membership = organizationService.addUser(
                ORG_ID, memberId, null, null, "INSTRUCTOR"
        );

        assertThat(membership.getRole()).isEqualTo(UserRole.INSTRUCTOR);
        assertThat(membership.getUserId()).isEqualTo(memberId);
    }

    @Test
    void addUser_invalidRole_throwsValidationError() {
        Organization org = new Organization(ORG_ID, "Acme", "acme", OrganizationStatus.ACTIVE, Instant.now());
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));

        assertThatThrownBy(() -> organizationService.addUser(ORG_ID, USER_ID, null, null, "ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void addUser_duplicateMembership_throwsConflict() {
        Organization org = new Organization(ORG_ID, "Acme", "acme", OrganizationStatus.ACTIVE, Instant.now());
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(platformUserRepository.findById(USER_ID)).thenReturn(Optional.of(
                new PlatformUser(USER_ID, "Owner", "owner@example.com", Instant.now())
        ));
        when(membershipRepository.existsByOrganizationIdAndUserId(ORG_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.addUser(ORG_ID, USER_ID, null, null, "STUDENT"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void create_savesMembershipForActor() {
        when(organizationRepository.existsBySlug(any())).thenReturn(false);
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(platformUserRepository.findById(USER_ID)).thenReturn(Optional.of(
                new PlatformUser(USER_ID, "Owner", "owner@example.com", Instant.now())
        ));
        when(membershipRepository.existsByOrganizationIdAndUserId(any(), any())).thenReturn(false);
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        organizationService.create("Nova Org");

        ArgumentCaptor<OrganizationMembership> captor = ArgumentCaptor.forClass(OrganizationMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CONTENT_OWNER);
    }
}
