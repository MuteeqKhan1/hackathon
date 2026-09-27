-- Source sections, section versions, chunks (PRD-04)
CREATE TABLE source_sections (
    id                   UUID PRIMARY KEY,
    source_material_id   UUID NOT NULL REFERENCES source_materials(id),
    parent_section_id    UUID,
    external_reference   VARCHAR(255) NOT NULL,
    title                VARCHAR(512) NOT NULL,
    section_type         VARCHAR(64) NOT NULL,
    CONSTRAINT uq_source_section_ref UNIQUE (source_material_id, external_reference)
);

CREATE INDEX idx_source_sections_material ON source_sections (source_material_id);

CREATE TABLE source_section_versions (
    id                   UUID PRIMARY KEY,
    source_section_id    UUID NOT NULL REFERENCES source_sections(id),
    source_version_id    UUID NOT NULL REFERENCES source_material_versions(id),
    content              TEXT NOT NULL,
    content_hash         VARCHAR(64) NOT NULL,
    change_type          VARCHAR(32) NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_section_version UNIQUE (source_section_id, source_version_id)
);

CREATE INDEX idx_section_versions_source_version ON source_section_versions (source_version_id);

CREATE TABLE source_chunks (
    id                        UUID PRIMARY KEY,
    source_section_version_id UUID NOT NULL REFERENCES source_section_versions(id),
    chunk_index               INT NOT NULL,
    content                   TEXT NOT NULL,
    embedding_ref             VARCHAR(255),
    metadata_json             TEXT NOT NULL,
    CONSTRAINT uq_chunk_index UNIQUE (source_section_version_id, chunk_index)
);

CREATE INDEX idx_source_chunks_section_version ON source_chunks (source_section_version_id);
