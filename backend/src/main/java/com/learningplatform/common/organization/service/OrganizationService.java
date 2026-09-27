package com.learningplatform.common.organization.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.exception.ConflictException;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.organization.domain.Organization;
import com.learningplatform.common.organization.domain.OrganizationMembership;
import com.learningplatform.common.organization.domain.OrganizationStatus;
import com.learningplatform.common.organization.domain.PlatformUser;
import com.learningplatform.common.organization.repository.OrganizationMembershipRepository;
import com.learningplatform.common.organization.repository.OrganizationRepository;
import com.learningplatform.common.organization.repository.PlatformUserRepository;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.common.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final PlatformUserRepository platformUserRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AuditService auditService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            PlatformUserRepository platformUserRepository,
            OrganizationMembershipRepository membershipRepository,
            AuditService auditService
    ) {
        this.organizationRepository = organizationRepository;
        this.platformUserRepository = platformUserRepository;
        this.membershipRepository = membershipRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Organization create(String name) {
        SecurityContext.require();
        String trimmed = requireName(name);
        String slug = SlugFactory.unique(SlugFactory.fromName(trimmed), organizationRepository::existsBySlug);

        Organization organization = new Organization(
                UUID.randomUUID(),
                trimmed,
                slug,
                OrganizationStatus.ACTIVE,
                Instant.now()
        );
        organizationRepository.save(organization);

        AuthenticatedUser actor = SecurityContext.require();
        ensureUserExists(actor.userId(), actor.userId() + "@local.dev", "User " + actor.userId().toString().substring(0, 8));
        if (!membershipRepository.existsByOrganizationIdAndUserId(organization.getId(), actor.userId())) {
            membershipRepository.save(new OrganizationMembership(
                    UUID.randomUUID(),
                    organization.getId(),
                    actor.userId(),
                    actor.role(),
                    Instant.now()
            ));
        }

        auditService.record("ORGANIZATION_CREATED", "Organization", organization.getId().toString(), organization.getName());
        return organization;
    }

    @Transactional(readOnly = true)
    public Organization getById(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization was not found."));
        PermissionGuard.requireSameOrganization(organization.getId());
        return organization;
    }

    @Transactional(readOnly = true)
    public List<Organization> listForCurrentUser() {
        AuthenticatedUser actor = SecurityContext.require();
        return membershipRepository.findByUserId(actor.userId()).stream()
                .map(m -> organizationRepository.findById(m.getOrganizationId()).orElse(null))
                .filter(o -> o != null)
                .toList();
    }

    @Transactional
    public OrganizationMembership addUser(UUID organizationId, UUID userId, String email, String displayName, String roleRaw) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization was not found."));
        PermissionGuard.requireSameOrganization(organization.getId());

        UserRole role = parseRole(roleRaw);
        UUID resolvedUserId = resolveOrCreateUser(userId, email, displayName);

        if (membershipRepository.existsByOrganizationIdAndUserId(organizationId, resolvedUserId)) {
            throw new ConflictException("MEMBERSHIP_EXISTS", "User is already a member of this organization.");
        }

        OrganizationMembership membership = new OrganizationMembership(
                UUID.randomUUID(),
                organizationId,
                resolvedUserId,
                role,
                Instant.now()
        );
        membershipRepository.save(membership);
        auditService.record(
                "ORGANIZATION_USER_ADDED",
                "OrganizationMembership",
                membership.getId().toString(),
                resolvedUserId + ":" + role.name()
        );
        return membership;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembership> listMembers(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("ORGANIZATION_NOT_FOUND", "Organization was not found."));
        PermissionGuard.requireSameOrganization(organization.getId());
        return membershipRepository.findByOrganizationId(organizationId);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name.trim();
    }

    private static UserRole parseRole(String roleRaw) {
        if (roleRaw == null || roleRaw.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        try {
            return UserRole.valueOf(roleRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role. Expected CONTENT_OWNER, INSTRUCTOR, or STUDENT");
        }
    }

    private UUID resolveOrCreateUser(UUID userId, String email, String displayName) {
        if (userId != null) {
            return platformUserRepository.findById(userId)
                    .map(PlatformUser::getId)
                    .orElseGet(() -> ensureUserExists(
                            userId,
                            email != null && !email.isBlank() ? email : userId + "@local.dev",
                            displayName != null && !displayName.isBlank() ? displayName : "User " + userId.toString().substring(0, 8)
                    ).getId());
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required when userId is omitted");
        }
        return platformUserRepository.findByEmailIgnoreCase(email.trim())
                .map(PlatformUser::getId)
                .orElseGet(() -> ensureUserExists(
                        UUID.randomUUID(),
                        email.trim(),
                        displayName != null && !displayName.isBlank() ? displayName.trim() : email.trim()
                ).getId());
    }

    private PlatformUser ensureUserExists(UUID userId, String email, String displayName) {
        return platformUserRepository.findById(userId).orElseGet(() ->
                platformUserRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                        platformUserRepository.save(new PlatformUser(userId, displayName, email, Instant.now()))
                )
        );
    }
}
