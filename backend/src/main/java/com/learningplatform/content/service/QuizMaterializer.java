package com.learningplatform.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.assessment.domain.Quiz;
import com.learningplatform.assessment.domain.QuizQuestion;
import com.learningplatform.assessment.repository.QuizQuestionRepository;
import com.learningplatform.assessment.repository.QuizRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class QuizMaterializer {

    private final ObjectMapper objectMapper;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public QuizMaterializer(
            ObjectMapper objectMapper,
            QuizRepository quizRepository,
            QuizQuestionRepository quizQuestionRepository
    ) {
        this.objectMapper = objectMapper;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    public void materialize(UUID contentAssetId, UUID contentAssetVersionId, String contentJson) {
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            Quiz quiz = new Quiz(
                    UUID.randomUUID(),
                    contentAssetId,
                    contentAssetVersionId,
                    root.path("title").asText("Quiz"),
                    Instant.now()
            );
            quizRepository.save(quiz);
            int seq = 1;
            for (JsonNode q : root.path("questions")) {
                quizQuestionRepository.save(new QuizQuestion(
                        UUID.randomUUID(),
                        quiz.getId(),
                        seq++,
                        q.path("prompt").asText(),
                        q.path("options").toString(),
                        q.path("correctIndex").asInt(0),
                        q.path("explanation").asText(null)
                ));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to materialize quiz rows: " + ex.getMessage(), ex);
        }
    }
}
