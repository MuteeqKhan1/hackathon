package com.learningplatform.source.domain;

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
        name = "source_material_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_source_version_number",
                columnNames = {"source_material_id", "version_number"}
        )
)
public class SourceMaterialVersion {

    @Id
    private UUID id;

    @Column(name = "source_material_id", nullable = false)
    private UUID sourceMaterialId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "file_content_hash", nullable = false, length = 64)
    private String fileContentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SourceVersionStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected SourceMaterialVersion() {
    }

    public SourceMaterialVersion(
            UUID id,
            UUID sourceMaterialId,
            int versionNumber,
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            String fileContentHash,
            SourceVersionStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant publishedAt
    ) {
        this.id = id;
        this.sourceMaterialId = sourceMaterialId;
        this.versionNumber = versionNumber;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.fileContentHash = fileContentHash;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public void markProcessing() {
        assertMutable();
        this.status = SourceVersionStatus.PROCESSING;
    }

    public void markPublished(Instant at) {
        if (this.status != SourceVersionStatus.PROCESSING && this.status != SourceVersionStatus.DRAFT) {
            throw new BusinessException("ILLEGAL_STATE", "Only DRAFT/PROCESSING versions can be published.");
        }
        this.status = SourceVersionStatus.PUBLISHED;
        this.publishedAt = at;
    }

    public void markFailed() {
        if (this.status == SourceVersionStatus.PUBLISHED) {
            throw new BusinessException("ILLEGAL_STATE", "Published versions are immutable.");
        }
        this.status = SourceVersionStatus.FAILED;
    }

    public void assertMutable() {
        if (this.status == SourceVersionStatus.PUBLISHED) {
            throw new BusinessException("ILLEGAL_STATE", "Published versions are immutable.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceMaterialId() {
        return sourceMaterialId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getFileContentHash() {
        return fileContentHash;
    }

    public SourceVersionStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
