package com.learningplatform.content.domain;

import com.learningplatform.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "content_asset_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_content_asset_version", columnNames = {"content_asset_id", "version_number"})
)
public class ContentAssetVersion {

    @Id
    private UUID id;

    @Column(name = "content_asset_id", nullable = false)
    private UUID contentAssetId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    @Column(nullable = false, length = 32)
    private String language;

    @Column(name = "source_version_id", nullable = false)
    private UUID sourceVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_method", nullable = false, length = 32)
    private GenerationMethod generationMethod;

    @Column(name = "prompt_version", nullable = false, length = 64)
    private String promptVersion;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContentAssetStatus status;

    @Column(name = "manual_modification", nullable = false)
    private boolean manualModification;

    @Column(name = "sync_policy", nullable = false, length = 32)
    private String syncPolicy;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected ContentAssetVersion() {
    }

    public ContentAssetVersion(
            UUID id,
            UUID contentAssetId,
            int versionNumber,
            String contentJson,
            String language,
            UUID sourceVersionId,
            GenerationMethod generationMethod,
            String promptVersion,
            String modelName,
            String modelVersion,
            ContentAssetStatus status,
            boolean manualModification,
            String syncPolicy,
            UUID createdBy,
            Instant createdAt
    ) {
        this.id = id;
        this.contentAssetId = contentAssetId;
        this.versionNumber = versionNumber;
        this.contentJson = contentJson;
        this.language = language;
        this.sourceVersionId = sourceVersionId;
        this.generationMethod = generationMethod;
        this.promptVersion = promptVersion;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.status = status;
        this.manualModification = manualModification;
        this.syncPolicy = syncPolicy;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContentAssetId() {
        return contentAssetId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getContentJson() {
        return contentJson;
    }

    public String getLanguage() {
        return language;
    }

    public UUID getSourceVersionId() {
        return sourceVersionId;
    }

    public GenerationMethod getGenerationMethod() {
        return generationMethod;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public ContentAssetStatus getStatus() {
        return status;
    }

    public boolean isManualModification() {
        return manualModification;
    }

    public String getSyncPolicy() {
        return syncPolicy;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void applyManualEdit(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            throw new IllegalArgumentException("contentJson must not be blank");
        }
        this.contentJson = contentJson;
        this.manualModification = true;
        this.status = ContentAssetStatus.PENDING_REVIEW;
        this.approvedBy = null;
        this.approvedAt = null;
    }

    public void approve(UUID approverId, Instant at) {
        if (this.status != ContentAssetStatus.PENDING_REVIEW && this.status != ContentAssetStatus.GENERATED) {
            throw new BusinessException("ILLEGAL_STATE", "Only PENDING_REVIEW/GENERATED versions can be approved.");
        }
        this.status = ContentAssetStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = at;
    }

    public void reject() {
        if (this.status != ContentAssetStatus.PENDING_REVIEW && this.status != ContentAssetStatus.GENERATED) {
            throw new BusinessException("ILLEGAL_STATE", "Only PENDING_REVIEW/GENERATED versions can be rejected.");
        }
        this.status = ContentAssetStatus.REJECTED;
        this.approvedBy = null;
        this.approvedAt = null;
    }

    public void setSyncPolicy(SyncPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("sync policy must not be null");
        }
        this.syncPolicy = policy.name();
    }

    public SyncPolicy getSyncPolicyEnum() {
        if (this.syncPolicy == null) {
            return SyncPolicy.AUTO;
        }
        if ("FOLLOW_SOURCE".equals(this.syncPolicy) || "AUTO".equals(this.syncPolicy)) {
            return SyncPolicy.AUTO;
        }
        try {
            return SyncPolicy.valueOf(this.syncPolicy);
        } catch (Exception ex) {
            return SyncPolicy.AUTO;
        }
    }
}
