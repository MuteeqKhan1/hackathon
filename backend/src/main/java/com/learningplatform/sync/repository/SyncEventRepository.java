package com.learningplatform.sync.repository;

import com.learningplatform.sync.domain.SyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncEventRepository extends JpaRepository<SyncEvent, UUID> {
    Optional<SyncEvent> findByIdempotencyKey(String idempotencyKey);

    List<SyncEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
