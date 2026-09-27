package com.learningplatform.student.repository;

import com.learningplatform.student.domain.StudentPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentPreferenceRepository extends JpaRepository<StudentPreference, UUID> {
    Optional<StudentPreference> findByStudentId(UUID studentId);
}
