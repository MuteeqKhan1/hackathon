package com.learningplatform.common.organization.api;

import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.organization.domain.Organization;
import com.learningplatform.common.organization.domain.OrganizationMembership;
import com.learningplatform.common.organization.domain.OrganizationStatus;
import com.learningplatform.common.organization.service.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return OrganizationResponse.from(organizationService.create(request.name()));
    }

    @GetMapping
    public List<OrganizationResponse> listMine() {
        return organizationService.listForCurrentUser().stream().map(OrganizationResponse::from).toList();
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(@PathVariable UUID organizationId) {
        return OrganizationResponse.from(organizationService.getById(organizationId));
    }

    @PostMapping("/{organizationId}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse addUser(
            @PathVariable UUID organizationId,
            @Valid @RequestBody AddOrganizationUserRequest request
    ) {
        return MembershipResponse.from(organizationService.addUser(
                organizationId,
                request.userId(),
                request.email(),
                request.displayName(),
                request.role()
        ));
    }

    @GetMapping("/{organizationId}/users")
    public List<MembershipResponse> listUsers(@PathVariable UUID organizationId) {
        return organizationService.listMembers(organizationId).stream().map(MembershipResponse::from).toList();
    }

    public record CreateOrganizationRequest(@NotBlank String name) {
    }

    public record AddOrganizationUserRequest(
            UUID userId,
            String email,
            String displayName,
            @NotBlank String role
    ) {
    }

    public record OrganizationResponse(
            UUID id,
            String name,
            String slug,
            OrganizationStatus status,
            Instant createdAt
    ) {
        static OrganizationResponse from(Organization organization) {
            return new OrganizationResponse(
                    organization.getId(),
                    organization.getName(),
                    organization.getSlug(),
                    organization.getStatus(),
                    organization.getCreatedAt()
            );
        }
    }

    public record MembershipResponse(
            UUID id,
            UUID organizationId,
            UUID userId,
            UserRole role,
            Instant createdAt
    ) {
        static MembershipResponse from(OrganizationMembership membership) {
            return new MembershipResponse(
                    membership.getId(),
                    membership.getOrganizationId(),
                    membership.getUserId(),
                    membership.getRole(),
                    membership.getCreatedAt()
            );
        }
    }
}
