-- Source materials + immutable versions (PRD-03)
CREATE TABLE source_materials (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    subject          VARCHAR(128),
    status           VARCHAR(32) NOT NULL,
    created_by       UUID NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_source_materials_org ON source_materials (organization_id);

CREATE TABLE source_material_versions (
    id                   UUID PRIMARY KEY,
    source_material_id   UUID NOT NULL REFERENCES source_materials(id),
    version_number       INT NOT NULL,
    storage_key          VARCHAR(512) NOT NULL,
    original_filename    VARCHAR(512) NOT NULL,
    content_type         VARCHAR(128) NOT NULL,
    file_size_bytes      BIGINT NOT NULL,
    file_content_hash    VARCHAR(64) NOT NULL,
    status               VARCHAR(32) NOT NULL,
    created_by           UUID NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_source_version_number UNIQUE (source_material_id, version_number)
);

CREATE INDEX idx_source_versions_material ON source_material_versions (source_material_id);
