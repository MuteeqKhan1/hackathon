package com.learningplatform.operation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operations")
public class AsyncOperation {

    @Id
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false, length = 128)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OperationStatus status;

    @Column(nullable = false)
    private int progress;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "result_json")
    private String resultJson;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AsyncOperation() {
    }

    public static AsyncOperation initiated(UUID orgId, String type, String correlationId) {
        Instant now = Instant.now();
        AsyncOperation op = new AsyncOperation();
        op.id = UUID.randomUUID();
        op.organizationId = orgId;
        op.type = type;
        op.status = OperationStatus.INITIATED;
        op.progress = 0;
        op.correlationId = correlationId;
        op.createdAt = now;
        op.updatedAt = now;
        return op;
    }

    public void markRunning(int progress) {
        this.status = OperationStatus.RUNNING;
        this.progress = progress;
        this.updatedAt = Instant.now();
    }

    public void markCompleted(String resultJson) {
        this.status = OperationStatus.COMPLETED;
        this.progress = 100;
        this.resultJson = resultJson;
        this.updatedAt = Instant.now();
        this.completedAt = this.updatedAt;
    }

    public void markFailed(String code, String message) {
        this.status = OperationStatus.FAILED;
        this.errorCode = code;
        this.errorMessage = message;
        this.updatedAt = Instant.now();
        this.completedAt = this.updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getType() {
        return type;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
