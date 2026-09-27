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
@Table(name = "sync_events")
public class SyncEvent {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "source_material_id", nullable = false)
    private UUID sourceMaterialId;

    @Column(name = "old_version_id", nullable = false)
    private UUID oldVersionId;

    @Column(name = "new_version_id", nullable = false)
    private UUID newVersionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SyncEventStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 256)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected SyncEvent() {
    }

    public SyncEvent(
            UUID id,
            UUID organizationId,
            UUID sourceMaterialId,
            UUID oldVersionId,
            UUID newVersionId,
            SyncEventStatus status,
            String idempotencyKey,
            Instant createdAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.sourceMaterialId = sourceMaterialId;
        this.oldVersionId = oldVersionId;
        this.newVersionId = newVersionId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public void transitionTo(SyncEventStatus next) {
        this.status = next;
        if (next == SyncEventStatus.COMPLETED || next == SyncEventStatus.FAILED) {
            this.completedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getSourceMaterialId() {
        return sourceMaterialId;
    }

    public UUID getOldVersionId() {
        return oldVersionId;
    }

    public UUID getNewVersionId() {
        return newVersionId;
    }

    public SyncEventStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
