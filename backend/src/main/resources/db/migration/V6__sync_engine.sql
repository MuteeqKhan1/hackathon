-- Sync engine + in-app notifications (PRD-09)

CREATE TABLE sync_events (
    id                   UUID PRIMARY KEY,
    organization_id      UUID NOT NULL,
    source_material_id   UUID NOT NULL REFERENCES source_materials(id),
    old_version_id       UUID NOT NULL REFERENCES source_material_versions(id),
    new_version_id       UUID NOT NULL REFERENCES source_material_versions(id),
    status               VARCHAR(32) NOT NULL,
    idempotency_key      VARCHAR(256) NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at         TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_sync_event_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_sync_events_org ON sync_events (organization_id);
CREATE INDEX idx_sync_events_material ON sync_events (source_material_id);

CREATE TABLE impact_analysis (
    id                    UUID PRIMARY KEY,
    sync_event_id         UUID NOT NULL REFERENCES sync_events(id) ON DELETE CASCADE,
    course_id             UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    content_asset_id      UUID NOT NULL REFERENCES content_assets(id) ON DELETE CASCADE,
    source_section_id     UUID,
    impact_level          VARCHAR(16) NOT NULL,
    reason                VARCHAR(512) NOT NULL,
    recommended_action    VARCHAR(32) NOT NULL,
    change_signature      VARCHAR(256),
    status                VARCHAR(32) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_impact_analysis_event ON impact_analysis (sync_event_id);
CREATE INDEX idx_impact_analysis_course ON impact_analysis (course_id);
CREATE INDEX idx_impact_analysis_asset ON impact_analysis (content_asset_id);

CREATE TABLE sync_actions (
    id                      UUID PRIMARY KEY,
    impact_analysis_id      UUID NOT NULL REFERENCES impact_analysis(id) ON DELETE CASCADE,
    action                  VARCHAR(32) NOT NULL,
    requested_by            UUID NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    old_asset_version_id    UUID,
    new_asset_version_id    UUID,
    detail                  VARCHAR(512),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at            TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_sync_actions_impact ON sync_actions (impact_analysis_id);

CREATE TABLE notifications (
    id                   UUID PRIMARY KEY,
    organization_id      UUID NOT NULL,
    user_id              UUID NOT NULL,
    type                 VARCHAR(64) NOT NULL,
    title                VARCHAR(256) NOT NULL,
    body                 TEXT NOT NULL,
    related_entity_type  VARCHAR(64),
    related_entity_id    UUID,
    read_at              TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications (organization_id, user_id, created_at DESC);
