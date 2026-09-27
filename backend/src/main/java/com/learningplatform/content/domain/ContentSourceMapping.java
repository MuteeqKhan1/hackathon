package com.learningplatform.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "content_source_mapping")
public class ContentSourceMapping {

    @Id
    private UUID id;

    @Column(name = "content_asset_id", nullable = false)
    private UUID contentAssetId;

    @Column(name = "content_asset_version_id", nullable = false)
    private UUID contentAssetVersionId;

    @Column(name = "source_section_id", nullable = false)
    private UUID sourceSectionId;

    @Column(name = "source_section_version_id", nullable = false)
    private UUID sourceSectionVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 32)
    private MappingRelationshipType relationshipType;

    protected ContentSourceMapping() {
    }

    public ContentSourceMapping(
            UUID id,
            UUID contentAssetId,
            UUID contentAssetVersionId,
            UUID sourceSectionId,
            UUID sourceSectionVersionId,
            MappingRelationshipType relationshipType
    ) {
        this.id = id;
        this.contentAssetId = contentAssetId;
        this.contentAssetVersionId = contentAssetVersionId;
        this.sourceSectionId = sourceSectionId;
        this.sourceSectionVersionId = sourceSectionVersionId;
        this.relationshipType = relationshipType;
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

    public UUID getSourceSectionId() {
        return sourceSectionId;
    }

    public UUID getSourceSectionVersionId() {
        return sourceSectionVersionId;
    }

    public MappingRelationshipType getRelationshipType() {
        return relationshipType;
    }
}
