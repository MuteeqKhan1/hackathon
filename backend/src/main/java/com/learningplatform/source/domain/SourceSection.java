package com.learningplatform.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "source_sections",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_source_section_ref",
                columnNames = {"source_material_id", "external_reference"}
        )
)
public class SourceSection {

    @Id
    private UUID id;

    @Column(name = "source_material_id", nullable = false)
    private UUID sourceMaterialId;

    @Column(name = "parent_section_id")
    private UUID parentSectionId;

    @Column(name = "external_reference", nullable = false)
    private String externalReference;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(name = "section_type", nullable = false, length = 64)
    private String sectionType;

    protected SourceSection() {
    }

    public SourceSection(
            UUID id,
            UUID sourceMaterialId,
            UUID parentSectionId,
            String externalReference,
            String title,
            String sectionType
    ) {
        this.id = id;
        this.sourceMaterialId = sourceMaterialId;
        this.parentSectionId = parentSectionId;
        this.externalReference = externalReference;
        this.title = title;
        this.sectionType = sectionType;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceMaterialId() {
        return sourceMaterialId;
    }

    public UUID getParentSectionId() {
        return parentSectionId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getTitle() {
        return title;
    }

    public String getSectionType() {
        return sectionType;
    }
}
