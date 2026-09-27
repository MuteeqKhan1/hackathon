package com.learningplatform.source.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialStatus;
import com.learningplatform.source.repository.SourceMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SourceMaterialService {

    private final SourceMaterialRepository sourceMaterialRepository;
    private final AuditService auditService;

    public SourceMaterialService(SourceMaterialRepository sourceMaterialRepository, AuditService auditService) {
        this.sourceMaterialRepository = sourceMaterialRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SourceMaterial create(String title, String description, String subject) {
        AuthenticatedUser user = PermissionGuard.require(Permission.SOURCE_CREATE);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        SourceMaterial material = new SourceMaterial(
                UUID.randomUUID(),
                user.organizationId(),
                title.trim(),
                description,
                subject,
                SourceMaterialStatus.ACTIVE,
                user.userId(),
                Instant.now()
        );
        sourceMaterialRepository.save(material);
        auditService.record("SOURCE_CREATED", "SourceMaterial", material.getId().toString(), material.getTitle());
        return material;
    }

    @Transactional(readOnly = true)
    public SourceMaterial get(UUID materialId) {
        PermissionGuard.require(Permission.SOURCE_VIEW);
        SourceMaterial material = sourceMaterialRepository.findById(materialId)
                .orElseThrow(() -> new NotFoundException("SOURCE_MATERIAL_NOT_FOUND", "Source material was not found."));
        PermissionGuard.requireSameOrganization(material.getOrganizationId());
        return material;
    }

    @Transactional(readOnly = true)
    public List<SourceMaterial> listForCurrentOrg() {
        AuthenticatedUser user = PermissionGuard.require(Permission.SOURCE_VIEW);
        return sourceMaterialRepository.findByOrganizationIdOrderByCreatedAtDesc(user.organizationId());
    }
}
