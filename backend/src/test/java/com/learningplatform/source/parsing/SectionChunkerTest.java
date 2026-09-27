package com.learningplatform.source.parsing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SectionChunkerTest {

    @Test
    void chunk_longSection_orderedContiguousIndexes() {
        String content = IntStream.range(0, 100)
                .mapToObj(i -> "Paragraph " + i + " about force and motion.")
                .collect(Collectors.joining("\n\n"));
        List<String> chunks = SectionChunker.chunk(content, 200);
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks.get(0)).isNotBlank();
        assertThat(String.join("", chunks).replace(" ", ""))
                .contains("Paragraph0");
    }
}
