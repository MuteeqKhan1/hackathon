package com.learningplatform.course.domain;

import com.learningplatform.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "source_material_id", nullable = false)
    private UUID sourceMaterialId;

    @Column(name = "current_source_version_id", nullable = false)
    private UUID currentSourceVersionId;

    @Column(nullable = false, length = 32)
    private String language;

    @Column(length = 128)
    private String audience;

    @Column(length = 64)
    private String level;

    @Column(name = "duration_hours")
    private BigDecimal durationHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CourseStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Course() {
    }

    public Course(
            UUID id,
            UUID organizationId,
            String title,
            String description,
            UUID sourceMaterialId,
            UUID currentSourceVersionId,
            String language,
            String audience,
            String level,
            BigDecimal durationHours,
            CourseStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.title = title;
        this.description = description;
        this.sourceMaterialId = sourceMaterialId;
        this.currentSourceVersionId = currentSourceVersionId;
        this.language = language;
        this.audience = audience;
        this.level = level;
        this.durationHours = durationHours;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void patch(String title, String description, String language, String audience, String level, BigDecimal durationHours) {
        if (title != null && !title.isBlank()) {
            this.title = title.trim();
        }
        if (description != null) {
            this.description = description;
        }
        if (language != null && !language.isBlank()) {
            this.language = language.trim();
        }
        if (audience != null) {
            this.audience = audience;
        }
        if (level != null) {
            this.level = level;
        }
        if (durationHours != null) {
            this.durationHours = durationHours;
        }
        touch();
    }

    public void markUpdateRequired() {
        if (this.status == CourseStatus.ARCHIVED) {
            throw new BusinessException("ILLEGAL_STATE", "Archived courses cannot become UPDATE_REQUIRED.");
        }
        this.status = CourseStatus.UPDATE_REQUIRED;
        touch();
    }

    public void assertEditableStructure() {
        if (this.status == CourseStatus.ARCHIVED) {
            throw new BusinessException("ILLEGAL_STATE", "Archived courses cannot change structure.");
        }
        if (this.status == CourseStatus.PUBLISHED) {
            throw new BusinessException("ILLEGAL_STATE", "Published course structure is locked until sync/review.");
        }
    }

    public void markPublished() {
        if (this.status == CourseStatus.ARCHIVED) {
            throw new BusinessException("ILLEGAL_STATE", "Archived courses cannot be published.");
        }
        this.status = CourseStatus.PUBLISHED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getSourceMaterialId() {
        return sourceMaterialId;
    }

    public UUID getCurrentSourceVersionId() {
        return currentSourceVersionId;
    }

    public String getLanguage() {
        return language;
    }

    public String getAudience() {
        return audience;
    }

    public String getLevel() {
        return level;
    }

    public BigDecimal getDurationHours() {
        return durationHours;
    }

    public CourseStatus getStatus() {
        return status;
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
