package com.learningplatform.assessment.scoring;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuizScorerTest {

    @Test
    void allCorrect_scores100() {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        Map<UUID, Integer> correct = Map.of(q1, 0, q2, 1);
        Map<UUID, Integer> selected = Map.of(q1, 0, q2, 1);
        assertThat(QuizScorer.score(correct, selected)).isEqualTo(100.0);
    }

    @Test
    void halfCorrect_scores50() {
        assertThat(QuizScorer.scoreFromLists(List.of(0, 1), List.of(0, 0))).isEqualTo(50.0);
    }
}
