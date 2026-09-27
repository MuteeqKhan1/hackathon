package com.learningplatform.student.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "student_mastery",
        uniqueConstraints = @UniqueConstraint(name = "uq_student_topic_mastery", columnNames = {"student_id", "topic_id"})
)
public class StudentMastery {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "mastery_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal masteryScore;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "average_quiz_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal averageQuizScore;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentMastery() {
    }

    public StudentMastery(
            UUID id,
            UUID organizationId,
            UUID studentId,
            UUID topicId,
            Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.studentId = studentId;
        this.topicId = topicId;
        this.masteryScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.attempts = 0;
        this.averageQuizScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.updatedAt = updatedAt;
    }

    public void recordAttempt(BigDecimal quizScore) {
        this.attempts += 1;
        BigDecimal total = this.averageQuizScore
                .multiply(BigDecimal.valueOf(this.attempts - 1))
                .add(quizScore);
        this.averageQuizScore = total.divide(BigDecimal.valueOf(this.attempts), 2, RoundingMode.HALF_UP);
        // mastery tracks average quiz score (no slow/fast labels)
        this.masteryScore = this.averageQuizScore;
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

    public UUID getTopicId() {
        return topicId;
    }

    public BigDecimal getMasteryScore() {
        return masteryScore;
    }

    public int getAttempts() {
        return attempts;
    }

    public BigDecimal getAverageQuizScore() {
        return averageQuizScore;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
