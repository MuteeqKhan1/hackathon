package com.learningplatform.assessment.repository;

import com.learningplatform.assessment.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {
    List<QuizQuestion> findByQuizIdOrderBySequenceAsc(UUID quizId);
}
