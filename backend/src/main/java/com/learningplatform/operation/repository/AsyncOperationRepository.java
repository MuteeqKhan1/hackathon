package com.learningplatform.operation.repository;

import com.learningplatform.operation.domain.AsyncOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AsyncOperationRepository extends JpaRepository<AsyncOperation, UUID> {
}
