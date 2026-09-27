package com.learningplatform.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "student_topic_progress",
        uniqueConstraints = @UniqueConstraint(name = "uq_student_topic_progress", columnNames = {"student_id", "topic_id"})
)
public class StudentTopicProgress {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "lesson_viewed", nullable = false)
    private boolean lessonViewed;

    @Column(name = "quiz_completed", nullable = false)
    private boolean quizCompleted;

    @Column(name = "percent_complete", nullable = false)
    private int percentComplete;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentTopicProgress() {
    }

    public StudentTopicProgress(
            UUID id,
            UUID organizationId,
            UUID studentId,
            UUID courseId,
            UUID topicId,
            Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.topicId = topicId;
        this.lessonViewed = false;
        this.quizCompleted = false;
        this.percentComplete = 0;
        this.updatedAt = updatedAt;
    }

    public void markLessonViewed() {
        this.lessonViewed = true;
        recalc();
    }

    public void markQuizCompleted() {
        this.quizCompleted = true;
        recalc();
    }

    private void recalc() {
        int p = 0;
        if (lessonViewed) {
            p += 50;
        }
        if (quizCompleted) {
            p += 50;
        }
        this.percentComplete = p;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public boolean isLessonViewed() {
        return lessonViewed;
    }

    public boolean isQuizCompleted() {
        return quizCompleted;
    }

    public int getPercentComplete() {
        return percentComplete;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
