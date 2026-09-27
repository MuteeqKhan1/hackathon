package com.learningplatform.assessment.repository;

import com.learningplatform.assessment.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
    Optional<Quiz> findByContentAssetVersionId(UUID contentAssetVersionId);

    Optional<Quiz> findFirstByContentAssetIdOrderByCreatedAtDesc(UUID contentAssetId);
}
