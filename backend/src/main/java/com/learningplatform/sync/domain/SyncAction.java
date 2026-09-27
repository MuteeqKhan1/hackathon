package com.learningplatform.sync.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sync_actions")
public class SyncAction {

    @Id
    private UUID id;

    @Column(name = "impact_analysis_id", nullable = false)
    private UUID impactAnalysisId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SyncActionType action;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SyncActionStatus status;

    @Column(name = "old_asset_version_id")
    private UUID oldAssetVersionId;

    @Column(name = "new_asset_version_id")
    private UUID newAssetVersionId;

    @Column(length = 512)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected SyncAction() {
    }

    public SyncAction(
            UUID id,
            UUID impactAnalysisId,
            SyncActionType action,
            UUID requestedBy,
            SyncActionStatus status,
            UUID oldAssetVersionId,
            Instant createdAt
    ) {
        this.id = id;
        this.impactAnalysisId = impactAnalysisId;
        this.action = action;
        this.requestedBy = requestedBy;
        this.status = status;
        this.oldAssetVersionId = oldAssetVersionId;
        this.createdAt = createdAt;
    }

    public void complete(UUID newAssetVersionId, String detail) {
        this.status = SyncActionStatus.COMPLETED;
        this.newAssetVersionId = newAssetVersionId;
        this.detail = detail;
        this.completedAt = Instant.now();
    }

    public void fail(String detail) {
        this.status = SyncActionStatus.FAILED;
        this.detail = detail;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getImpactAnalysisId() {
        return impactAnalysisId;
    }

    public SyncActionType getAction() {
        return action;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public SyncActionStatus getStatus() {
        return status;
    }

    public UUID getOldAssetVersionId() {
        return oldAssetVersionId;
    }

    public UUID getNewAssetVersionId() {
        return newAssetVersionId;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
