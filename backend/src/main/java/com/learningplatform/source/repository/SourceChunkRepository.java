package com.learningplatform.source.repository;

import com.learningplatform.source.domain.SourceChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SourceChunkRepository extends JpaRepository<SourceChunk, UUID> {
    List<SourceChunk> findBySourceSectionVersionIdOrderByChunkIndexAsc(UUID sectionVersionId);
}
