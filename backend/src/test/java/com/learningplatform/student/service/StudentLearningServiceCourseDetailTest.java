package com.learningplatform.student.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.assessment.repository.QuizQuestionRepository;
import com.learningplatform.assessment.repository.QuizRepository;
import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.content.domain.AssetType;
import com.learningplatform.content.domain.ContentAsset;
import com.learningplatform.content.domain.ContentAssetStatus;
import com.learningplatform.content.repository.ContentAssetRepository;
import com.learningplatform.content.repository.ContentAssetVersionRepository;
import com.learningplatform.content.video.VideoMediaService;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.domain.StructureNodeStatus;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import com.learningplatform.student.repository.StudentQuizAttemptRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentLearningServiceCourseDetailTest {

    private static final UUID ORG = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID READY_TOPIC = UUID.randomUUID();
    private static final UUID DRAFT_TOPIC = UUID.randomUUID();

    @Mock private StudentCourseQuery studentCourseQuery;
    @Mock private CourseChapterRepository chapterRepository;
    @Mock private CourseTopicRepository topicRepository;
    @Mock private ContentAssetRepository assetRepository;
    @Mock private ContentAssetVersionRepository versionRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private QuizQuestionRepository quizQuestionRepository;
    @Mock private StudentQuizAttemptRepository attemptRepository;
    @Mock private ProgressService progressService;
    @Mock private MasteryService masteryService;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;
    @Mock private StudentPreferenceService preferenceService;
    @Mock private LessonPersonalizer lessonPersonalizer;
    @Mock private VideoMediaService videoMediaService;

    @InjectMocks
    private StudentLearningService service;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG, UserRole.STUDENT, Permission.forRole(UserRole.STUDENT)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void courseDetail_onlyIncludesTopicsWithApprovedExplanationAndQuiz() {
        Course course = new Course(
                COURSE_ID, ORG, "Physics 101", "", UUID.randomUUID(), UUID.randomUUID(),
                "en", null, null, null, CourseStatus.PUBLISHED, USER, Instant.now(), Instant.now()
        );
        when(studentCourseQuery.getPublished(COURSE_ID)).thenReturn(course);
        when(chapterRepository.findByCourseIdOrderBySequenceAsc(COURSE_ID))
                .thenReturn(List.of(new CourseChapter(CHAPTER_ID, COURSE_ID, "Ch1", 1, StructureNodeStatus.DRAFT)));
        when(topicRepository.findByChapterIdOrderBySequenceAsc(CHAPTER_ID)).thenReturn(List.of(
                new CourseTopic(READY_TOPIC, CHAPTER_ID, "Ready topic", 1, StructureNodeStatus.DRAFT),
                new CourseTopic(DRAFT_TOPIC, CHAPTER_ID, "Not ready", 2, StructureNodeStatus.DRAFT)
        ));

        stubApproved(READY_TOPIC, AssetType.EXPLANATION);
        stubApproved(READY_TOPIC, AssetType.QUIZ);
        when(assetRepository.findByTopicIdAndAssetType(DRAFT_TOPIC, AssetType.EXPLANATION)).thenReturn(Optional.empty());

        StudentLearningService.StudentCourseDetail detail = service.courseDetail(COURSE_ID);

        assertThat(detail.chapters()).hasSize(1);
        assertThat(detail.chapters().get(0).topics()).extracting(StudentLearningService.TopicNode::title)
                .containsExactly("Ready topic");
    }

    private void stubApproved(UUID topicId, AssetType type) {
        UUID assetId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ContentAsset asset = new ContentAsset(
                assetId, ORG, COURSE_ID, CHAPTER_ID, topicId, type,
                ContentAssetStatus.APPROVED, USER, Instant.now(), Instant.now()
        );
        asset.setCurrentVersion(versionId, ContentAssetStatus.APPROVED);
        when(assetRepository.findByTopicIdAndAssetType(topicId, type)).thenReturn(Optional.of(asset));
    }
}
