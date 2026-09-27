package com.learningplatform.course.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes a course when required EXPLANATION+QUIZ assets are APPROVED (PRD-07).
 */
@Service
public class CoursePublishService {

    private static final Set<AssetType> REQUIRED_TYPES = EnumSet.of(AssetType.EXPLANATION, AssetType.QUIZ);

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;
    private final ContentAssetRepository contentAssetRepository;
    private final AuditService auditService;

    public CoursePublishService(
            CourseService courseService,
            CourseRepository courseRepository,
            CourseChapterRepository chapterRepository,
            CourseTopicRepository topicRepository,
            ContentAssetRepository contentAssetRepository,
            AuditService auditService
    ) {
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
        this.contentAssetRepository = contentAssetRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Course publish(UUID courseId, boolean allowIncomplete) {
        PermissionGuard.require(Permission.COURSE_PUBLISH);
        Course course = courseService.get(courseId);

        List<String> blockers = findRequiredBlockers(courseId);
        if (!blockers.isEmpty() && !allowIncomplete) {
            int shown = Math.min(blockers.size(), 8);
            String sample = String.join("; ", blockers.subList(0, shown));
            String more = blockers.size() > shown ? " … +" + (blockers.size() - shown) + " more" : "";
            throw new BusinessException(
                    "COURSE_PUBLISH_BLOCKED",
                    "Required content not APPROVED (" + blockers.size() + "): " + sample + more
                            + ". Use Publish (allow incomplete) after approving a few topics, or regenerate a smaller structure."
            );
        }

        course.markPublished();
        courseRepository.save(course);

        String auditDetail = allowIncomplete
                ? "allowIncomplete=true;pending=" + blockers.size()
                : "all_required_approved";
        auditService.record("COURSE_PUBLISHED", "Course", course.getId().toString(), auditDetail);
        if (allowIncomplete && !blockers.isEmpty()) {
            int shown = Math.min(blockers.size(), 20);
            auditService.record(
                    "COURSE_PUBLISH_INCOMPLETE_ALLOWED",
                    "Course",
                    course.getId().toString(),
                    String.join("|", blockers.subList(0, shown))
                            + (blockers.size() > shown ? "|…+" + (blockers.size() - shown) : "")
            );
        }
        return course;
    }

    /** Convenience for callers that omit the flag. */
    @Transactional
    public Course publish(UUID courseId) {
        return publish(courseId, false);
    }

    private List<String> findRequiredBlockers(UUID courseId) {
        List<String> blockers = new ArrayList<>();
        List<CourseChapter> chapters = chapterRepository.findByCourseIdOrderBySequenceAsc(courseId);
        for (CourseChapter chapter : chapters) {
            List<CourseTopic> topics = topicRepository.findByChapterIdOrderBySequenceAsc(chapter.getId());
            for (CourseTopic topic : topics) {
                for (AssetType type : REQUIRED_TYPES) {
                    ContentAsset asset = contentAssetRepository
                            .findByTopicIdAndAssetType(topic.getId(), type)
                            .orElse(null);
                    if (asset == null || asset.getStatus() != ContentAssetStatus.APPROVED) {
                        String status = asset == null ? "MISSING" : asset.getStatus().name();
                        blockers.add(topic.getTitle() + "/" + type.name() + "=" + status);
                    }
                }
            }
        }
        return blockers;
    }
}
