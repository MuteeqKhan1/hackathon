package com.learningplatform.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "prompt_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_prompt_key_version", columnNames = {"prompt_key", "version"})
)
public class PromptVersion {

    @Id
    private UUID id;

    @Column(name = "prompt_key", nullable = false, length = 128)
    private String promptKey;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String template;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromptVersion() {
    }

    public PromptVersion(UUID id, String promptKey, String version, String template, Instant createdAt) {
        this.id = id;
        this.promptKey = promptKey;
        this.version = version;
        this.template = template;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public String getVersion() {
        return version;
    }

    public String getTemplate() {
        return template;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
