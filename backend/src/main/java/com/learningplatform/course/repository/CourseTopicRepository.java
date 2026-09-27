package com.learningplatform.course.repository;

import com.learningplatform.course.domain.CourseTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseTopicRepository extends JpaRepository<CourseTopic, UUID> {
    List<CourseTopic> findByChapterIdOrderBySequenceAsc(UUID chapterId);

    Optional<CourseTopic> findByIdAndChapterId(UUID id, UUID chapterId);

    void deleteByChapterIdIn(List<UUID> chapterIds);
}
