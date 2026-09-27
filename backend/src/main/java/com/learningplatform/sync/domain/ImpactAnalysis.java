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
@Table(name = "impact_analysis")
public class ImpactAnalysis {

    @Id
    private UUID id;

    @Column(name = "sync_event_id", nullable = false)
    private UUID syncEventId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "content_asset_id", nullable = false)
    private UUID contentAssetId;

    @Column(name = "source_section_id")
    private UUID sourceSectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_level", nullable = false, length = 16)
    private ImpactLevel impactLevel;

    @Column(nullable = false, length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 32)
    private RecommendedAction recommendedAction;

    @Column(name = "change_signature", length = 256)
    private String changeSignature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImpactStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ImpactAnalysis() {
    }

    public ImpactAnalysis(
            UUID id,
            UUID syncEventId,
            UUID courseId,
            UUID contentAssetId,
            UUID sourceSectionId,
            ImpactLevel impactLevel,
            String reason,
            RecommendedAction recommendedAction,
            String changeSignature,
            ImpactStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.syncEventId = syncEventId;
        this.courseId = courseId;
        this.contentAssetId = contentAssetId;
        this.sourceSectionId = sourceSectionId;
        this.impactLevel = impactLevel;
        this.reason = reason;
        this.recommendedAction = recommendedAction;
        this.changeSignature = changeSignature;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void close() {
        this.status = ImpactStatus.CLOSED;
    }

    public void mergeFrom(ImpactLevel otherLevel, String otherReason, RecommendedAction otherAction, String otherSignature) {
        if (otherLevel != null && otherLevel.ordinal() > this.impactLevel.ordinal()) {
            this.impactLevel = otherLevel;
        }
        if (otherAction == RecommendedAction.REVIEW_CONFLICT) {
            this.recommendedAction = RecommendedAction.REVIEW_CONFLICT;
        }
        if (otherReason != null && !otherReason.isBlank()) {
            if (this.reason == null || this.reason.isBlank()) {
                this.reason = otherReason;
            } else if (!this.reason.contains(otherReason)) {
                String merged = this.reason + "; " + otherReason;
                this.reason = merged.length() <= 500 ? merged : merged.substring(0, 497) + "…";
            }
        }
        if (otherSignature != null && !otherSignature.isBlank()) {
            this.changeSignature = otherSignature;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSyncEventId() {
        return syncEventId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getContentAssetId() {
        return contentAssetId;
    }

    public UUID getSourceSectionId() {
        return sourceSectionId;
    }

    public ImpactLevel getImpactLevel() {
        return impactLevel;
    }

    public String getReason() {
        return reason;
    }

    public RecommendedAction getRecommendedAction() {
        return recommendedAction;
    }

    public String getChangeSignature() {
        return changeSignature;
    }

    public ImpactStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
