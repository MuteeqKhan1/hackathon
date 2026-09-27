package com.learningplatform.course.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reorder chapters/topics while preserving entity ids (UT-05-04).
 */
@Service
public class ChapterReorderService {

    private final CourseService courseService;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;

    public ChapterReorderService(
            CourseService courseService,
            CourseChapterRepository chapterRepository,
            CourseTopicRepository topicRepository
    ) {
        this.courseService = courseService;
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public List<CourseTopic> moveTopicToIndex(UUID courseId, UUID chapterId, UUID topicId, int targetIndex) {
        PermissionGuard.require(Permission.COURSE_EDIT);
        Course course = courseService.get(courseId);
        course.assertEditableStructure();

        CourseChapter chapter = chapterRepository.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new NotFoundException("CHAPTER_NOT_FOUND", "Chapter was not found."));

        List<CourseTopic> topics = new ArrayList<>(topicRepository.findByChapterIdOrderBySequenceAsc(chapter.getId()));
        CourseTopic moving = topics.stream()
                .filter(t -> t.getId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("TOPIC_NOT_FOUND", "Topic was not found in chapter."));

        topics.remove(moving);
        int index = Math.max(0, Math.min(targetIndex, topics.size()));
        topics.add(index, moving);

        // Two-phase resequence to satisfy unique (chapter_id, sequence) constraint
        for (int i = 0; i < topics.size(); i++) {
            topics.get(i).setSequence(-(i + 1));
        }
        topicRepository.saveAll(topics);
        topicRepository.flush();

        for (int i = 0; i < topics.size(); i++) {
            topics.get(i).setSequence(i + 1);
        }
        topicRepository.saveAll(topics);
        return topicRepository.findByChapterIdOrderBySequenceAsc(chapter.getId());
    }

    @Transactional
    public List<CourseChapter> reorderChapters(UUID courseId, List<UUID> orderedChapterIds) {
        PermissionGuard.require(Permission.COURSE_EDIT);
        Course course = courseService.get(courseId);
        course.assertEditableStructure();

        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderBySequenceAsc(courseId);
        validatePermutation(chapters.stream().map(CourseChapter::getId).toList(), orderedChapterIds);

        Map<UUID, CourseChapter> byId = chapters.stream()
                .collect(Collectors.toMap(CourseChapter::getId, Function.identity()));

        for (int i = 0; i < orderedChapterIds.size(); i++) {
            byId.get(orderedChapterIds.get(i)).setSequence(-(i + 1));
        }
        chapterRepository.saveAll(byId.values());
        chapterRepository.flush();

        for (int i = 0; i < orderedChapterIds.size(); i++) {
            byId.get(orderedChapterIds.get(i)).setSequence(i + 1);
        }
        chapterRepository.saveAll(byId.values());
        return chapterRepository.findByCourseIdOrderBySequenceAsc(courseId);
    }

    private static void validatePermutation(List<UUID> existing, List<UUID> ordered) {
        if (ordered == null || ordered.size() != existing.size()) {
            throw new BusinessException("INVALID_REORDER", "Reorder list must include every chapter exactly once.");
        }
        Set<UUID> seen = new HashSet<>();
        for (UUID id : ordered) {
            if (!seen.add(id) || !existing.contains(id)) {
                throw new BusinessException("INVALID_REORDER", "Reorder list must include every chapter exactly once.");
            }
        }
    }
}
