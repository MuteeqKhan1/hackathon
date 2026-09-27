package com.learningplatform.course.repository;

import com.learningplatform.course.domain.CourseChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseChapterRepository extends JpaRepository<CourseChapter, UUID> {
    List<CourseChapter> findByCourseIdOrderBySequenceAsc(UUID courseId);

    Optional<CourseChapter> findByIdAndCourseId(UUID id, UUID courseId);

    void deleteByCourseId(UUID courseId);
}
