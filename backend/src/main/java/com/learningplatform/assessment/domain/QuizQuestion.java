package com.learningplatform.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "quiz_questions",
        uniqueConstraints = @UniqueConstraint(name = "uq_quiz_question_seq", columnNames = {"quiz_id", "sequence"})
)
public class QuizQuestion {

    @Id
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_index", nullable = false)
    private int correctIndex;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    protected QuizQuestion() {
    }

    public QuizQuestion(
            UUID id,
            UUID quizId,
            int sequence,
            String prompt,
            String optionsJson,
            int correctIndex,
            String explanation
    ) {
        this.id = id;
        this.quizId = quizId;
        this.sequence = sequence;
        this.prompt = prompt;
        this.optionsJson = optionsJson;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getQuizId() {
        return quizId;
    }

    public int getSequence() {
        return sequence;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getExplanation() {
        return explanation;
    }
}
