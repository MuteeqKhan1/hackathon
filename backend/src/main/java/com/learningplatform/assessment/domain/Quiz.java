package com.learningplatform.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    private UUID id;

    @Column(name = "content_asset_id", nullable = false)
    private UUID contentAssetId;

    @Column(name = "content_asset_version_id", nullable = false)
    private UUID contentAssetVersionId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Quiz() {
    }

    public Quiz(UUID id, UUID contentAssetId, UUID contentAssetVersionId, String title, Instant createdAt) {
        this.id = id;
        this.contentAssetId = contentAssetId;
        this.contentAssetVersionId = contentAssetVersionId;
        this.title = title;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContentAssetId() {
        return contentAssetId;
    }

    public UUID getContentAssetVersionId() {
        return contentAssetVersionId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
