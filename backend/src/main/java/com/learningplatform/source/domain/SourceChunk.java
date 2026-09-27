package com.learningplatform.source.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "source_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_chunk_index",
                columnNames = {"source_section_version_id", "chunk_index"}
        )
)
public class SourceChunk {

    @Id
    private UUID id;

    @Column(name = "source_section_version_id", nullable = false)
    private UUID sourceSectionVersionId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "embedding_ref")
    private String embeddingRef;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "TEXT")
    private String metadataJson;

    protected SourceChunk() {
    }

    public SourceChunk(
            UUID id,
            UUID sourceSectionVersionId,
            int chunkIndex,
            String content,
            String embeddingRef,
            String metadataJson
    ) {
        this.id = id;
        this.sourceSectionVersionId = sourceSectionVersionId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embeddingRef = embeddingRef;
        this.metadataJson = metadataJson;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceSectionVersionId() {
        return sourceSectionVersionId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public String getEmbeddingRef() {
        return embeddingRef;
    }

    public String getMetadataJson() {
        return metadataJson;
    }
}
