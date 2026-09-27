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
        name = "course_topics",
        uniqueConstraints = @UniqueConstraint(name = "uq_course_topic_seq", columnNames = {"chapter_id", "sequence"})
)
public class CourseTopic {

    @Id
    private UUID id;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StructureNodeStatus status;

    protected CourseTopic() {
    }

    public CourseTopic(UUID id, UUID chapterId, String title, int sequence, StructureNodeStatus status) {
        this.id = id;
        this.chapterId = chapterId;
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

    public void moveToChapter(UUID chapterId) {
        this.chapterId = chapterId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getChapterId() {
        return chapterId;
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
