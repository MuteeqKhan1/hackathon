package com.learningplatform.sync.repository;

import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactLevel;
import com.learningplatform.sync.domain.ImpactStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImpactAnalysisRepository extends JpaRepository<ImpactAnalysis, UUID> {
    List<ImpactAnalysis> findBySyncEventId(UUID syncEventId);

    List<ImpactAnalysis> findByCourseIdAndStatusAndImpactLevel(
            UUID courseId, ImpactStatus status, ImpactLevel impactLevel
    );

    List<ImpactAnalysis> findByContentAssetIdAndStatusOrderByCreatedAtDesc(
            UUID contentAssetId, ImpactStatus status
    );
}
