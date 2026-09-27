package com.learningplatform.student.service;

import com.learningplatform.content.domain.LearningPace;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentPreferenceServiceTest {

    @Test
    void resolveLanguage_fallsBackToCourse() {
        assertThat(StudentPreferenceService.resolveLanguage(null, "en")).isEqualTo("en");
        assertThat(StudentPreferenceService.resolveLanguage("hi", "en")).isEqualTo("hi");
    }

    @Test
    void resolvePace_defaultsMedium() {
        assertThat(StudentPreferenceService.resolvePace(null)).isEqualTo(LearningPace.MEDIUM);
        assertThat(StudentPreferenceService.resolvePace("EASY")).isEqualTo(LearningPace.EASY);
    }
}
