-- Content assets + lineage (PRD-06)
CREATE TABLE prompt_versions (
    id          UUID PRIMARY KEY,
    prompt_key  VARCHAR(128) NOT NULL,
    version     VARCHAR(32) NOT NULL,
    template    TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_prompt_key_version UNIQUE (prompt_key, version)
);

CREATE TABLE content_assets (
    id                   UUID PRIMARY KEY,
    organization_id      UUID NOT NULL,
    course_id            UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    chapter_id           UUID NOT NULL REFERENCES course_chapters(id) ON DELETE CASCADE,
    topic_id             UUID NOT NULL REFERENCES course_topics(id) ON DELETE CASCADE,
    asset_type           VARCHAR(32) NOT NULL,
    status               VARCHAR(32) NOT NULL,
    current_version_id   UUID,
    created_by           UUID NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_content_asset_topic_type UNIQUE (topic_id, asset_type)
);

CREATE INDEX idx_content_assets_course ON content_assets (course_id);
CREATE INDEX idx_content_assets_topic ON content_assets (topic_id);

CREATE TABLE content_asset_versions (
    id                       UUID PRIMARY KEY,
    content_asset_id         UUID NOT NULL REFERENCES content_assets(id) ON DELETE CASCADE,
    version_number           INT NOT NULL,
    content_json             TEXT NOT NULL,
    language                 VARCHAR(32) NOT NULL,
    source_version_id        UUID NOT NULL REFERENCES source_material_versions(id),
    generation_method        VARCHAR(32) NOT NULL,
    prompt_version           VARCHAR(64) NOT NULL,
    model_name               VARCHAR(128) NOT NULL,
    model_version            VARCHAR(64) NOT NULL,
    status                   VARCHAR(32) NOT NULL,
    manual_modification      BOOLEAN NOT NULL DEFAULT FALSE,
    sync_policy              VARCHAR(32) NOT NULL DEFAULT 'FOLLOW_SOURCE',
    created_by               UUID NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by              UUID,
    approved_at              TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_content_asset_version UNIQUE (content_asset_id, version_number)
);

CREATE INDEX idx_content_asset_versions_asset ON content_asset_versions (content_asset_id);

CREATE TABLE content_source_mapping (
    id                          UUID PRIMARY KEY,
    content_asset_id            UUID NOT NULL REFERENCES content_assets(id) ON DELETE CASCADE,
    content_asset_version_id    UUID NOT NULL REFERENCES content_asset_versions(id) ON DELETE CASCADE,
    source_section_id           UUID NOT NULL REFERENCES source_sections(id),
    source_section_version_id   UUID NOT NULL REFERENCES source_section_versions(id),
    relationship_type           VARCHAR(32) NOT NULL
);

CREATE INDEX idx_content_source_mapping_asset ON content_source_mapping (content_asset_id);
CREATE INDEX idx_content_source_mapping_section ON content_source_mapping (source_section_id);

CREATE TABLE quizzes (
    id                      UUID PRIMARY KEY,
    content_asset_id        UUID NOT NULL REFERENCES content_assets(id) ON DELETE CASCADE,
    content_asset_version_id UUID NOT NULL REFERENCES content_asset_versions(id) ON DELETE CASCADE,
    title                   VARCHAR(512) NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE quiz_questions (
    id              UUID PRIMARY KEY,
    quiz_id         UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    sequence        INT NOT NULL,
    prompt          TEXT NOT NULL,
    options_json    TEXT NOT NULL,
    correct_index   INT NOT NULL,
    explanation     TEXT,
    CONSTRAINT uq_quiz_question_seq UNIQUE (quiz_id, sequence)
);

-- Seed prompt templates
INSERT INTO prompt_versions (id, prompt_key, version, template, created_at) VALUES
(
    'a1000000-0000-0000-0000-000000000001',
    'EXPLANATION_GENERATION',
    '1.0.0',
    'Generate a grounded explanation JSON for topic "{{topicTitle}}" using only the provided source chunks. Include citedSectionIds from the chunk metadata. Schema: title, body, keyPoints[], citedSectionIds[].',
    CURRENT_TIMESTAMP
),
(
    'a1000000-0000-0000-0000-000000000002',
    'QUIZ_GENERATION',
    '1.0.0',
    'Generate a grounded quiz JSON for topic "{{topicTitle}}" using only the provided source chunks. Schema: title, questions[{prompt, options[4], correctIndex, explanation, citedSectionIds[]}].',
    CURRENT_TIMESTAMP
);
