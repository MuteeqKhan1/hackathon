package com.learningplatform.source.parsing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void normalize_spacesAndNewlines_sameResult() {
        String a = TextNormalizer.normalize("Hello   world\r\n\r\nNext");
        String b = TextNormalizer.normalize("Hello world\n\nNext");
        assertThat(a).isEqualTo(b);
    }
}
