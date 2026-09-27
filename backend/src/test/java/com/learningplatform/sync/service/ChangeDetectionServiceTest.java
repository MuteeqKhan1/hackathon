package com.learningplatform.sync.service;

import com.learningplatform.source.parsing.SectionChangeType;
import com.learningplatform.source.repository.SourceSectionVersionRepository;
import com.learningplatform.sync.domain.ImpactLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChangeDetectionServiceTest {

    @Mock private SourceSectionVersionRepository sectionVersionRepository;

    @InjectMocks
    private ChangeDetectionService service;

    @Test
    void smallTypoDelta_scoresLow() {
        ImpactLevel level = service.score(
                SectionChangeType.MODIFIED,
                "Newton's second law describes acceleration.",
                "Newton's second law describes accelerations."
        );
        assertThat(level).isEqualTo(ImpactLevel.LOW);
    }

    @Test
    void formulaChange_scoresHigh() {
        ImpactLevel level = service.score(
                SectionChangeType.MODIFIED,
                "The formula is F = ma",
                "The formula is F = 2ma"
        );
        assertThat(level).isEqualTo(ImpactLevel.HIGH);
    }

    @Test
    void sectionRemoved_scoresHigh() {
        ImpactLevel level = service.score(SectionChangeType.REMOVED, "old body", "");
        assertThat(level).isEqualTo(ImpactLevel.HIGH);
    }

    @Test
    void metadataChanged_scoresLow() {
        assertThat(service.score(SectionChangeType.METADATA_CHANGED, "a", "a"))
                .isEqualTo(ImpactLevel.LOW);
    }
}
