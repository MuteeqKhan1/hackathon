-- Student learning (PRD-10)

CREATE TABLE student_course_enrollments (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL,
    student_id       UUID NOT NULL,
    course_id        UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    enrolled_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_course_enrollment UNIQUE (student_id, course_id)
);

CREATE INDEX idx_enrollments_student ON student_course_enrollments (organization_id, student_id);

CREATE TABLE student_topic_progress (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL,
    student_id       UUID NOT NULL,
    course_id        UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    topic_id         UUID NOT NULL REFERENCES course_topics(id) ON DELETE CASCADE,
    lesson_viewed    BOOLEAN NOT NULL DEFAULT FALSE,
    quiz_completed   BOOLEAN NOT NULL DEFAULT FALSE,
    percent_complete INT NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_topic_progress UNIQUE (student_id, topic_id)
);

CREATE INDEX idx_topic_progress_course ON student_topic_progress (student_id, course_id);

CREATE TABLE student_quiz_attempts (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL,
    student_id       UUID NOT NULL,
    quiz_id          UUID NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    course_id        UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    topic_id         UUID NOT NULL REFERENCES course_topics(id) ON DELETE CASCADE,
    score            NUMERIC(5, 2) NOT NULL,
    answers_json     TEXT NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quiz_attempts_student ON student_quiz_attempts (student_id, quiz_id);

CREATE TABLE student_mastery (
    id                   UUID PRIMARY KEY,
    organization_id      UUID NOT NULL,
    student_id           UUID NOT NULL,
    topic_id             UUID NOT NULL REFERENCES course_topics(id) ON DELETE CASCADE,
    mastery_score        NUMERIC(5, 2) NOT NULL DEFAULT 0,
    attempts             INT NOT NULL DEFAULT 0,
    average_quiz_score   NUMERIC(5, 2) NOT NULL DEFAULT 0,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_topic_mastery UNIQUE (student_id, topic_id)
);
