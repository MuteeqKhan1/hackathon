package com.learningplatform.source.repository;

import com.learningplatform.source.domain.SourceSectionVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SourceSectionVersionRepository extends JpaRepository<SourceSectionVersion, UUID> {
    List<SourceSectionVersion> findBySourceVersionId(UUID sourceVersionId);
}
