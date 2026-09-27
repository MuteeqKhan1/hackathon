package com.learningplatform.content.repository;

import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentAssetRepository extends JpaRepository<ContentAsset, UUID> {
    Optional<ContentAsset> findByTopicIdAndAssetType(UUID topicId, AssetType assetType);

    List<ContentAsset> findByCourseIdOrderByCreatedAtAsc(UUID courseId);

    List<ContentAsset> findByTopicIdOrderByCreatedAtAsc(UUID topicId);

    List<ContentAsset> findByCourseIdAndStatusOrderByUpdatedAtDesc(UUID courseId, ContentAssetStatus status);
}
