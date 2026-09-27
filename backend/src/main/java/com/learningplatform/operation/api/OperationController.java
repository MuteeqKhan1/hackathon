package com.learningplatform.operation.api;

import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.domain.OperationStatus;
import com.learningplatform.operation.service.OperationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @GetMapping("/{operationId}")
    public OperationResponse get(@PathVariable UUID operationId) {
        return OperationResponse.from(operationService.get(operationId));
    }

    public record OperationResponse(
            UUID operationId,
            String type,
            OperationStatus status,
            int progress,
            String errorCode,
            String errorMessage,
            String resultJson,
            Instant createdAt,
            Instant completedAt
    ) {
        static OperationResponse from(AsyncOperation op) {
            return new OperationResponse(
                    op.getId(),
                    op.getType(),
                    op.getStatus(),
                    op.getProgress(),
                    op.getErrorCode(),
                    op.getErrorMessage(),
                    op.getResultJson(),
                    op.getCreatedAt(),
                    op.getCompletedAt()
            );
        }
    }
}
