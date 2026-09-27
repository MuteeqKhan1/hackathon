package com.learningplatform.source.repository;

import com.learningplatform.source.domain.SourceSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceSectionRepository extends JpaRepository<SourceSection, UUID> {
    Optional<SourceSection> findBySourceMaterialIdAndExternalReference(UUID materialId, String externalReference);

    List<SourceSection> findBySourceMaterialIdOrderByExternalReferenceAsc(UUID materialId);
}
