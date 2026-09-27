package com.learningplatform.course.domain;

/**
 * Course lifecycle (FR-CRS-03 / PRD-05). Publish gate lives in PRD-07.
 */
public enum CourseStatus {
    DRAFT,
    IN_REVIEW,
    PUBLISHED,
    UPDATE_REQUIRED,
    ARCHIVED
}
