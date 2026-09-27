package com.learningplatform.content.repository;

import com.learningplatform.content.domain.ContentSourceMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentSourceMappingRepository extends JpaRepository<ContentSourceMapping, UUID> {
    List<ContentSourceMapping> findByContentAssetVersionId(UUID contentAssetVersionId);

    List<ContentSourceMapping> findByContentAssetId(UUID contentAssetId);

    List<ContentSourceMapping> findBySourceSectionId(UUID sourceSectionId);
}
