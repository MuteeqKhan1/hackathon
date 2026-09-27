package com.learningplatform.course.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.domain.StructureNodeStatus;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoursePublishServiceTest {

    private static final UUID ORG = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COURSE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();

    @Mock private CourseService courseService;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseChapterRepository chapterRepository;
    @Mock private CourseTopicRepository topicRepository;
    @Mock private ContentAssetRepository contentAssetRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private CoursePublishService coursePublishService;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG, UserRole.INSTRUCTOR, Permission.forRole(UserRole.INSTRUCTOR)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void publish_requiredPending_blocked() {
        Course course = draftCourse();
        when(courseService.get(COURSE_ID)).thenReturn(course);
        stubTopics();
        when(contentAssetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.EXPLANATION))
                .thenReturn(Optional.of(asset(AssetType.EXPLANATION, ContentAssetStatus.PENDING_REVIEW)));
        when(contentAssetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.QUIZ))
                .thenReturn(Optional.of(asset(AssetType.QUIZ, ContentAssetStatus.APPROVED)));

        assertThatThrownBy(() -> coursePublishService.publish(COURSE_ID, false))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("COURSE_PUBLISH_BLOCKED"));
    }

    @Test
    void publish_allRequiredApproved_publishes() {
        Course course = draftCourse();
        when(courseService.get(COURSE_ID)).thenReturn(course);
        stubTopics();
        when(contentAssetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.EXPLANATION))
                .thenReturn(Optional.of(asset(AssetType.EXPLANATION, ContentAssetStatus.APPROVED)));
        when(contentAssetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.QUIZ))
                .thenReturn(Optional.of(asset(AssetType.QUIZ, ContentAssetStatus.APPROVED)));
        when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Course published = coursePublishService.publish(COURSE_ID, false);

        assertThat(published.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        verify(auditService).record(eq("COURSE_PUBLISHED"), eq("Course"), eq(COURSE_ID.toString()), any());
    }

    @Test
    void publish_allowIncomplete_publishesWithAuditFlag() {
        Course course = draftCourse();
        when(courseService.get(COURSE_ID)).thenReturn(course);
        stubTopics();
        when(contentAssetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.EXPLANATION))
                .thenReturn(Optional.empty());
        when(contentAssetRepository.findByTopicIdAndAssetType(TOPIC_ID, AssetType.QUIZ))
                .thenReturn(Optional.empty());
        when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Course published = coursePublishService.publish(COURSE_ID, true);

        assertThat(published.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        verify(auditService).record(eq("COURSE_PUBLISH_INCOMPLETE_ALLOWED"), any(), any(), any());
    }

    private void stubTopics() {
        when(chapterRepository.findByCourseIdOrderBySequenceAsc(COURSE_ID))
                .thenReturn(List.of(new CourseChapter(CHAPTER_ID, COURSE_ID, "Ch1", 1, StructureNodeStatus.DRAFT)));
        when(topicRepository.findByChapterIdOrderBySequenceAsc(CHAPTER_ID))
                .thenReturn(List.of(new CourseTopic(TOPIC_ID, CHAPTER_ID, "Newton", 1, StructureNodeStatus.DRAFT)));
    }

    private Course draftCourse() {
        return new Course(
                COURSE_ID, ORG, "Physics", null, UUID.randomUUID(), UUID.randomUUID(),
                "en", null, null, null, CourseStatus.DRAFT, USER, Instant.now(), Instant.now()
        );
    }

    private ContentAsset asset(AssetType type, ContentAssetStatus status) {
        return new ContentAsset(
                UUID.randomUUID(), ORG, COURSE_ID, CHAPTER_ID, TOPIC_ID,
                type, status, USER, Instant.now(), Instant.now()
        );
    }
}
