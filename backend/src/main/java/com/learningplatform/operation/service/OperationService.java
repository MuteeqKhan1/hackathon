package com.learningplatform.operation.service;

import com.learningplatform.common.correlation.CorrelationIdFilter;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.repository.AsyncOperationRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OperationService {

    private final AsyncOperationRepository asyncOperationRepository;

    public OperationService(AsyncOperationRepository asyncOperationRepository) {
        this.asyncOperationRepository = asyncOperationRepository;
    }

    @Transactional
    public AsyncOperation create(String type) {
        AuthenticatedUser user = SecurityContext.require();
        AsyncOperation op = AsyncOperation.initiated(
                user.organizationId(),
                type,
                MDC.get(CorrelationIdFilter.MDC_KEY)
        );
        return asyncOperationRepository.save(op);
    }

    @Transactional(readOnly = true)
    public AsyncOperation get(UUID operationId) {
        AsyncOperation op = asyncOperationRepository.findById(operationId)
                .orElseThrow(() -> new NotFoundException("OPERATION_NOT_FOUND", "Operation was not found."));
        if (op.getOrganizationId() != null) {
            PermissionGuard.requireSameOrganization(op.getOrganizationId());
        }
        return op;
    }

    @Transactional
    public void markRunning(UUID operationId, int progress) {
        AsyncOperation op = asyncOperationRepository.findById(operationId)
                .orElseThrow(() -> new NotFoundException("OPERATION_NOT_FOUND", "Operation was not found."));
        op.markRunning(progress);
        asyncOperationRepository.save(op);
    }

    @Transactional
    public void markCompleted(UUID operationId, String resultJson) {
        AsyncOperation op = asyncOperationRepository.findById(operationId)
                .orElseThrow(() -> new NotFoundException("OPERATION_NOT_FOUND", "Operation was not found."));
        op.markCompleted(resultJson);
        asyncOperationRepository.save(op);
    }

    @Transactional
    public void markFailed(UUID operationId, String code, String message) {
        AsyncOperation op = asyncOperationRepository.findById(operationId)
                .orElseThrow(() -> new NotFoundException("OPERATION_NOT_FOUND", "Operation was not found."));
        op.markFailed(code, message);
        asyncOperationRepository.save(op);
    }
}
