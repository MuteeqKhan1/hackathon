package com.learningplatform.assessment.scoring;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure MCQ scorer (PRD-10 / UT-10-03, UT-10-04). Zero Spring dependencies.
 */
public final class QuizScorer {

    private QuizScorer() {
    }

    /**
     * @param correctByQuestionId map of questionId → correct option index
     * @param selectedByQuestionId map of questionId → selected option index
     * @return score 0–100 as percentage of correctly answered questions among those with a correct key
     */
    public static double score(
            Map<UUID, Integer> correctByQuestionId,
            Map<UUID, Integer> selectedByQuestionId
    ) {
        if (correctByQuestionId == null || correctByQuestionId.isEmpty()) {
            return 0.0;
        }
        int total = correctByQuestionId.size();
        int correct = 0;
        for (Map.Entry<UUID, Integer> entry : correctByQuestionId.entrySet()) {
            Integer selected = selectedByQuestionId != null ? selectedByQuestionId.get(entry.getKey()) : null;
            if (selected != null && selected.equals(entry.getValue())) {
                correct++;
            }
        }
        return (correct * 100.0) / total;
    }

    public static double scoreFromLists(List<Integer> correctIndexes, List<Integer> selectedIndexes) {
        if (correctIndexes == null || correctIndexes.isEmpty()) {
            return 0.0;
        }
        int total = correctIndexes.size();
        int correct = 0;
        for (int i = 0; i < total; i++) {
            Integer selected = selectedIndexes != null && i < selectedIndexes.size() ? selectedIndexes.get(i) : null;
            if (selected != null && selected.equals(correctIndexes.get(i))) {
                correct++;
            }
        }
        return (correct * 100.0) / total;
    }
}
