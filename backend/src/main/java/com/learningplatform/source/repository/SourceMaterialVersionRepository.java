package com.learningplatform.source.repository;

import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceMaterialVersionRepository extends JpaRepository<SourceMaterialVersion, UUID> {

    List<SourceMaterialVersion> findBySourceMaterialIdOrderByVersionNumberAsc(UUID sourceMaterialId);

    Optional<SourceMaterialVersion> findByIdAndSourceMaterialId(UUID id, UUID sourceMaterialId);

    @Query("select coalesce(max(v.versionNumber), 0) from SourceMaterialVersion v where v.sourceMaterialId = :materialId")
    int findMaxVersionNumber(@Param("materialId") UUID materialId);

    Optional<SourceMaterialVersion> findFirstBySourceMaterialIdAndStatusAndIdNotOrderByVersionNumberDesc(
            UUID sourceMaterialId,
            SourceVersionStatus status,
            UUID excludeId
    );
}
