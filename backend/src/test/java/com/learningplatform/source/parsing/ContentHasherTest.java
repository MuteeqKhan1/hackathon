package com.learningplatform.source.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentHasherTest {

    @Test
    void sha256_sameNormalizedInput_identical() {
        String h1 = ContentHasher.sha256Normalized("Force  equals   mass");
        String h2 = ContentHasher.sha256Normalized("Force equals mass");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void sha256_oneCharacterChange_different() {
        String h1 = ContentHasher.sha256Normalized("F = ma");
        String h2 = ContentHasher.sha256Normalized("F = ma!");
        assertThat(h1).isNotEqualTo(h2);
    }
}
