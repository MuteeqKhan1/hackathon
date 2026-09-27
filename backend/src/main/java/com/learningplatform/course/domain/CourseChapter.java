package com.learningplatform.course.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "course_chapters",
        uniqueConstraints = @UniqueConstraint(name = "uq_course_chapter_seq", columnNames = {"course_id", "sequence"})
)
public class CourseChapter {

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StructureNodeStatus status;

    protected CourseChapter() {
    }

    public CourseChapter(UUID id, UUID courseId, String title, int sequence, StructureNodeStatus status) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.sequence = sequence;
        this.status = status;
    }

    public void rename(String title) {
        this.title = title.trim();
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public int getSequence() {
        return sequence;
    }

    public StructureNodeStatus getStatus() {
        return status;
    }
}
