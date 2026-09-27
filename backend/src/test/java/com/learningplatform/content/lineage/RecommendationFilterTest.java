package com.learningplatform.content.lineage;

import com.learningplatform.content.domain.MappingRelationshipType;
import com.learningplatform.content.domain.SyncPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationFilterTest {

    private final RecommendationFilter filter = new RecommendationFilter();

    @Test
    void customRelationship_excludedFromAutoRegenerate() {
        assertThat(filter.includeForAutoRegenerate(
                MappingRelationshipType.CUSTOM, SyncPolicy.AUTO, "a", "b"
        )).isFalse();
    }

    @Test
    void manualPolicy_sameChangeSignature_suppressed() {
        assertThat(filter.includeForAutoRegenerate(
                MappingRelationshipType.PRIMARY, SyncPolicy.MANUAL, "sig-1", "sig-1"
        )).isFalse();
    }

    @Test
    void primaryAuto_newSignature_included() {
        assertThat(filter.includeForAutoRegenerate(
                MappingRelationshipType.PRIMARY, SyncPolicy.AUTO, "sig-1", "sig-2"
        )).isTrue();
    }
}
