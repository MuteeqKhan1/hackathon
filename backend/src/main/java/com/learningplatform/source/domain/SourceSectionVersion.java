package com.learningplatform.source.domain;

import com.learningplatform.source.parsing.SectionChangeType;
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
        name = "source_section_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_section_version",
                columnNames = {"source_section_id", "source_version_id"}
        )
)
public class SourceSectionVersion {

    @Id
    private UUID id;

    @Column(name = "source_section_id", nullable = false)
    private UUID sourceSectionId;

    @Column(name = "source_version_id", nullable = false)
    private UUID sourceVersionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 32)
    private SectionChangeType changeType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SourceSectionVersion() {
    }

    public SourceSectionVersion(
            UUID id,
            UUID sourceSectionId,
            UUID sourceVersionId,
            String content,
            String contentHash,
            SectionChangeType changeType,
            Instant createdAt
    ) {
        this.id = id;
        this.sourceSectionId = sourceSectionId;
        this.sourceVersionId = sourceVersionId;
        this.content = content;
        this.contentHash = contentHash;
        this.changeType = changeType;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceSectionId() {
        return sourceSectionId;
    }

    public UUID getSourceVersionId() {
        return sourceVersionId;
    }

    public String getContent() {
        return content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public SectionChangeType getChangeType() {
        return changeType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
