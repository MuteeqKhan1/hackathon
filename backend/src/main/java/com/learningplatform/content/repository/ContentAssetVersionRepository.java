package com.learningplatform.content.repository;

import com.learningplatform.content.domain.ContentAssetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentAssetVersionRepository extends JpaRepository<ContentAssetVersion, UUID> {
    List<ContentAssetVersion> findByContentAssetIdOrderByVersionNumberAsc(UUID contentAssetId);

    Optional<ContentAssetVersion> findByIdAndContentAssetId(UUID id, UUID contentAssetId);

    @Query("select coalesce(max(v.versionNumber), 0) from ContentAssetVersion v where v.contentAssetId = :assetId")
    int findMaxVersionNumber(@Param("assetId") UUID assetId);
}
