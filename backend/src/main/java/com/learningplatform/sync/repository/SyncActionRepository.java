package com.learningplatform.sync.repository;

import com.learningplatform.sync.domain.SyncAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SyncActionRepository extends JpaRepository<SyncAction, UUID> {
}
