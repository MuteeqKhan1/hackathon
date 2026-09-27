package com.learningplatform.source.domain;

import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceMaterialVersionTest {

    @Test
    void assertMutable_published_rejects() {
        SourceMaterialVersion version = publishedVersion();
        assertThatThrownBy(version::assertMutable)
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ILLEGAL_STATE");
    }

    @Test
    void markProcessing_onPublished_rejects() {
        SourceMaterialVersion version = publishedVersion();
        assertThatThrownBy(version::markProcessing)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void markPublished_fromDraft_setsPublishedAt() {
        SourceMaterialVersion version = draftVersion();
        Instant at = Instant.parse("2026-01-01T00:00:00Z");
        version.markProcessing();
        version.markPublished(at);
        assertThat(version.getStatus()).isEqualTo(SourceVersionStatus.PUBLISHED);
        assertThat(version.getPublishedAt()).isEqualTo(at);
    }

    private static SourceMaterialVersion draftVersion() {
        return new SourceMaterialVersion(
                UUID.randomUUID(), UUID.randomUUID(), 1, "key", "a.pdf", "application/pdf",
                10, "abc", SourceVersionStatus.DRAFT, UUID.randomUUID(), Instant.now(), null
        );
    }

    private static SourceMaterialVersion publishedVersion() {
        return new SourceMaterialVersion(
                UUID.randomUUID(), UUID.randomUUID(), 1, "key", "a.pdf", "application/pdf",
                10, "abc", SourceVersionStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now()
        );
    }
}
