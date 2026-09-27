package com.learningplatform.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_quiz_attempts")
public class StudentQuizAttempt {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "answers_json", nullable = false, columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StudentQuizAttempt() {
    }

    public StudentQuizAttempt(
            UUID id,
            UUID organizationId,
            UUID studentId,
            UUID quizId,
            UUID courseId,
            UUID topicId,
            BigDecimal score,
            String answersJson,
            Instant createdAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.studentId = studentId;
        this.quizId = quizId;
        this.courseId = courseId;
        this.topicId = topicId;
        this.score = score;
        this.answersJson = answersJson;
        this.createdAt = createdAt;
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

    public UUID getQuizId() {
        return quizId;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getAnswersJson() {
        return answersJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
