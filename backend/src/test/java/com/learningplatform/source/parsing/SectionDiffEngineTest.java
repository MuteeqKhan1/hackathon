package com.learningplatform.source.parsing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SectionDiffEngineTest {

    @Test
    void diff_identical_onlyUnchanged() {
        var snap = new SectionDiffEngine.SectionSnapshot("SEC-1-a", "A", "hash1");
        List<SectionDiffEngine.SectionDiff> diffs = SectionDiffEngine.diff(
                Map.of("SEC-1-a", snap),
                Map.of("SEC-1-a", snap)
        );
        assertThat(diffs).hasSize(1);
        assertThat(diffs.get(0).changeType()).isEqualTo(SectionChangeType.UNCHANGED);
    }

    @Test
    void diff_textChangedSameKey_modified() {
        var oldS = new SectionDiffEngine.SectionSnapshot("SEC-1-a", "A", "hash1");
        var newS = new SectionDiffEngine.SectionSnapshot("SEC-1-a", "A", "hash2");
        assertThat(SectionDiffEngine.diff(Map.of("SEC-1-a", oldS), Map.of("SEC-1-a", newS)))
                .anyMatch(d -> d.changeType() == SectionChangeType.MODIFIED);
    }

    @Test
    void diff_keyOnlyInNew_added() {
        var neu = new SectionDiffEngine.SectionSnapshot("SEC-2-b", "B", "h");
        assertThat(SectionDiffEngine.diff(Map.of(), Map.of("SEC-2-b", neu)))
                .anyMatch(d -> d.changeType() == SectionChangeType.ADDED);
    }

    @Test
    void diff_keyOnlyInOld_removed() {
        var old = new SectionDiffEngine.SectionSnapshot("SEC-1-a", "A", "h");
        assertThat(SectionDiffEngine.diff(Map.of("SEC-1-a", old), Map.of()))
                .anyMatch(d -> d.changeType() == SectionChangeType.REMOVED);
    }

    @Test
    void diff_titleChangeSameHash_metadataChanged() {
        var oldS = new SectionDiffEngine.SectionSnapshot("SEC-1-a", "Old", "same");
        var newS = new SectionDiffEngine.SectionSnapshot("SEC-1-a", "New", "same");
        assertThat(SectionDiffEngine.diff(Map.of("SEC-1-a", oldS), Map.of("SEC-1-a", newS)))
                .anyMatch(d -> d.changeType() == SectionChangeType.METADATA_CHANGED);
    }
}
