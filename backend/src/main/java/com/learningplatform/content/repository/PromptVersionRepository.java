package com.learningplatform.content.repository;

import com.learningplatform.content.domain.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, UUID> {
    Optional<PromptVersion> findFirstByPromptKeyOrderByCreatedAtDesc(String promptKey);
}
