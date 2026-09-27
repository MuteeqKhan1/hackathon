-- Courses + structure (PRD-05)
CREATE TABLE courses (
    id                          UUID PRIMARY KEY,
    organization_id             UUID NOT NULL,
    title                       VARCHAR(255) NOT NULL,
    description                 TEXT,
    source_material_id          UUID NOT NULL REFERENCES source_materials(id),
    current_source_version_id   UUID NOT NULL REFERENCES source_material_versions(id),
    language                    VARCHAR(32) NOT NULL,
    audience                    VARCHAR(128),
    level                       VARCHAR(64),
    duration_hours              NUMERIC(6, 2),
    status                      VARCHAR(32) NOT NULL,
    created_by                  UUID NOT NULL,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_courses_org ON courses (organization_id);
CREATE INDEX idx_courses_source ON courses (source_material_id);

CREATE TABLE course_chapters (
    id          UUID PRIMARY KEY,
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(512) NOT NULL,
    sequence    INT NOT NULL,
    status      VARCHAR(32) NOT NULL,
    CONSTRAINT uq_course_chapter_seq UNIQUE (course_id, sequence)
);

CREATE INDEX idx_course_chapters_course ON course_chapters (course_id);

CREATE TABLE course_topics (
    id          UUID PRIMARY KEY,
    chapter_id  UUID NOT NULL REFERENCES course_chapters(id) ON DELETE CASCADE,
    title       VARCHAR(512) NOT NULL,
    sequence    INT NOT NULL,
    status      VARCHAR(32) NOT NULL,
    CONSTRAINT uq_course_topic_seq UNIQUE (chapter_id, sequence)
);

CREATE INDEX idx_course_topics_chapter ON course_topics (chapter_id);
