package com.learningplatform.student.domain;

import com.learningplatform.content.domain.LearningPace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "student_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uq_student_preferences", columnNames = "student_id")
)
public class StudentPreference {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "preferred_language", nullable = false, length = 16)
    private String preferredLanguage;

    @Column(name = "preferred_pace", nullable = false, length = 16)
    private String preferredPace;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentPreference() {
    }

    public StudentPreference(UUID id, UUID organizationId, UUID studentId, Instant updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.studentId = studentId;
        this.preferredLanguage = "en";
        this.preferredPace = LearningPace.MEDIUM.name();
        this.updatedAt = updatedAt;
    }

    public void update(String language, String pace) {
        if (language != null && !language.isBlank()) {
            this.preferredLanguage = language.trim().toLowerCase();
        }
        if (pace != null && !pace.isBlank()) {
            this.preferredPace = LearningPace.fromString(pace).name();
        }
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

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public String getPreferredPace() {
        return preferredPace;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
