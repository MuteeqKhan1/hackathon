package com.learningplatform.student.repository;

import com.learningplatform.student.domain.StudentQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentQuizAttemptRepository extends JpaRepository<StudentQuizAttempt, UUID> {
    List<StudentQuizAttempt> findByStudentIdAndQuizIdOrderByCreatedAtDesc(UUID studentId, UUID quizId);
}
