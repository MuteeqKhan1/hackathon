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
        name = "student_course_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uq_student_course_enrollment", columnNames = {"student_id", "course_id"})
)
public class StudentCourseEnrollment {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    protected StudentCourseEnrollment() {
    }

    public StudentCourseEnrollment(UUID id, UUID organizationId, UUID studentId, UUID courseId, Instant enrolledAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrolledAt = enrolledAt;
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

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
