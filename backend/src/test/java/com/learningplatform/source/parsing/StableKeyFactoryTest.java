package com.learningplatform.source.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StableKeyFactoryTest {

    @Test
    void fromOutlinePath_sameInputs_stable() {
        assertThat(StableKeyFactory.fromOutlinePath(1, "Newton's Laws"))
                .isEqualTo(StableKeyFactory.fromOutlinePath(1, "Newton's Laws"))
                .isEqualTo("CH1/newton-s-laws");
    }

    @Test
    void fromSequence_stable() {
        assertThat(StableKeyFactory.fromSequence(3, "Force")).isEqualTo("SEC-3-force");
    }
}
