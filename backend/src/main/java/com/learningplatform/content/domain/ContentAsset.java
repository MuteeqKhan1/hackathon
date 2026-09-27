package com.learningplatform.content.domain;

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
        name = "content_assets",
        uniqueConstraints = @UniqueConstraint(name = "uq_content_asset_topic_type", columnNames = {"topic_id", "asset_type"})
)
public class ContentAsset {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 32)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContentAssetStatus status;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentAsset() {
    }

    public ContentAsset(
            UUID id,
            UUID organizationId,
            UUID courseId,
            UUID chapterId,
            UUID topicId,
            AssetType assetType,
            ContentAssetStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.courseId = courseId;
        this.chapterId = chapterId;
        this.topicId = topicId;
        this.assetType = assetType;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setCurrentVersion(UUID versionId, ContentAssetStatus status) {
        this.currentVersionId = versionId;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getChapterId() {
        return chapterId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public ContentAssetStatus getStatus() {
        return status;
    }

    public UUID getCurrentVersionId() {
        return currentVersionId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
