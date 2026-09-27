package com.learningplatform.common.organization.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlugFactoryTest {

    @Test
    void fromName_normalizesToSlug() {
        assertThat(SlugFactory.fromName("  Physics Grade 10! ")).isEqualTo("physics-grade-10");
    }

    @Test
    void fromName_blank_throws() {
        assertThatThrownBy(() -> SlugFactory.fromName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void unique_whenBaseFree_returnsBase() {
        assertThat(SlugFactory.unique("acme", s -> false)).isEqualTo("acme");
    }

    @Test
    void unique_whenBaseTaken_appendsSuffix() {
        String result = SoftSlugFactory_uniqueTaken();
        assertThat(result).startsWith("acme-");
        assertThat(result).isNotEqualTo("acme");
    }

    private static String SoftSlugFactory_uniqueTaken() {
        return SlugFactory.unique("acme", "acme"::equals);
    }
}
