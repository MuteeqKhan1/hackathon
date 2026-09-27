package com.learningplatform.student.repository;

import com.learningplatform.student.domain.StudentMastery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentMasteryRepository extends JpaRepository<StudentMastery, UUID> {
    Optional<StudentMastery> findByStudentIdAndTopicId(UUID studentId, UUID topicId);
}
