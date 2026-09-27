package com.learningplatform.student.repository;

import com.learningplatform.student.domain.StudentTopicProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentTopicProgressRepository extends JpaRepository<StudentTopicProgress, UUID> {
    Optional<StudentTopicProgress> findByStudentIdAndTopicId(UUID studentId, UUID topicId);

    List<StudentTopicProgress> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
