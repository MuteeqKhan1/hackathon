-- Video prompt seed + student preferences (PRD-11/12/13)

INSERT INTO prompt_versions (id, prompt_key, version, template, created_at) VALUES
(
    'a1000000-0000-0000-0000-000000000003',
    'VIDEO_GENERATION',
    '1.0.0',
    'Generate a grounded video storyboard JSON for topic "{{topicTitle}}". Schema: title, pace, scenes[{sequence, narration, onScreenText, durationSeconds, citedSectionIds[]}], citedSectionIds[]. Use only provided source chunks.',
    CURRENT_TIMESTAMP
);

CREATE TABLE student_preferences (
    id                   UUID PRIMARY KEY,
    organization_id      UUID NOT NULL,
    student_id           UUID NOT NULL,
    preferred_language   VARCHAR(16) NOT NULL DEFAULT 'en',
    preferred_pace       VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_preferences UNIQUE (student_id)
);

CREATE INDEX idx_student_preferences_org ON student_preferences (organization_id, student_id);
