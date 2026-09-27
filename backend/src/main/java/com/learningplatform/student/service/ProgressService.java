package com.learningplatform.student.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.student.domain.StudentTopicProgress;
import com.learningplatform.student.repository.StudentTopicProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressService {

    private final StudentTopicProgressRepository progressRepository;

    public ProgressService(StudentTopicProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    @Transactional
    public StudentTopicProgress markLessonViewed(UUID courseId, UUID topicId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.PROGRESS_VIEW);
        StudentTopicProgress progress = getOrCreate(user, courseId, topicId);
        progress.markLessonViewed();
        return progressRepository.save(progress);
    }

    @Transactional
    public StudentTopicProgress markQuizCompleted(UUID courseId, UUID topicId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.PROGRESS_VIEW);
        StudentTopicProgress progress = getOrCreate(user, courseId, topicId);
        progress.markQuizCompleted();
        return progressRepository.save(progress);
    }

    @Transactional(readOnly = true)
    public CourseProgressView courseProgress(UUID courseId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.PROGRESS_VIEW);
        List<StudentTopicProgress> topics = progressRepository.findByStudentIdAndCourseId(user.userId(), courseId);
        int avg = 0;
        if (!topics.isEmpty()) {
            avg = (int) Math.round(topics.stream().mapToInt(StudentTopicProgress::getPercentComplete).average().orElse(0));
        }
        return new CourseProgressView(courseId, avg, topics);
    }

    /** Pure aggregate helper for unit tests. */
    public static int aggregateCoursePercent(List<StudentTopicProgress> topics) {
        if (topics == null || topics.isEmpty()) {
            return 0;
        }
        return (int) Math.round(topics.stream().mapToInt(StudentTopicProgress::getPercentComplete).average().orElse(0));
    }

    private StudentTopicProgress getOrCreate(AuthenticatedUser user, UUID courseId, UUID topicId) {
        return progressRepository.findByStudentIdAndTopicId(user.userId(), topicId)
                .orElseGet(() -> new StudentTopicProgress(
                        UUID.randomUUID(),
                        user.organizationId(),
                        user.userId(),
                        courseId,
                        topicId,
                        Instant.now()
                ));
    }

    public record CourseProgressView(UUID courseId, int coursePercent, List<StudentTopicProgress> topics) {
    }
}
