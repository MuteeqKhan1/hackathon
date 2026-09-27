package com.learningplatform.source.repository;

import com.learningplatform.source.domain.SourceMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SourceMaterialRepository extends JpaRepository<SourceMaterial, UUID> {
    List<SourceMaterial> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
