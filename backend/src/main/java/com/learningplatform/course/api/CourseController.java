package com.learningplatform.course.api;

import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseChapter;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.domain.CourseTopic;
import com.learningplatform.course.domain.StructureNodeStatus;
import com.learningplatform.course.repository.CourseChapterRepository;
import com.learningplatform.course.repository.CourseTopicRepository;
import com.learningplatform.course.service.ChapterReorderService;
import com.learningplatform.course.service.CourseBootstrapService;
import com.learningplatform.course.service.CoursePublishService;
import com.learningplatform.course.service.CourseService;
import com.learningplatform.course.service.CourseStructureService;
import com.learningplatform.operation.domain.AsyncOperation;
import com.learningplatform.operation.domain.OperationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseStructureService courseStructureService;
    private final CourseBootstrapService courseBootstrapService;
    private final ChapterReorderService chapterReorderService;
    private final CoursePublishService coursePublishService;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;

    public CourseController(
            CourseService courseService,
            CourseStructureService courseStructureService,
            CourseBootstrapService courseBootstrapService,
            ChapterReorderService chapterReorderService,
            CoursePublishService coursePublishService,
            CourseChapterRepository chapterRepository,
            CourseTopicRepository topicRepository
    ) {
        this.courseService = courseService;
        this.courseStructureService = courseStructureService;
        this.courseBootstrapService = courseBootstrapService;
        this.chapterReorderService = chapterReorderService;
        this.coursePublishService = coursePublishService;
        this.chapterRepository = chapterRepository;
        this.topicRepository = topicRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseCreateResponse create(@Valid @RequestBody CreateCourseRequest request) {
        Course course = courseService.create(
                request.title(),
                request.description(),
                request.sourceMaterialId(),
                request.sourceVersionId(),
                request.language(),
                request.audience(),
                request.level(),
                request.durationHours()
        );
        boolean generateDrafts = request.generateDrafts() == null || request.generateDrafts();
        AsyncOperation bootstrap = courseBootstrapService.maybeStartStructure(course.getId(), generateDrafts);
        return CourseCreateResponse.from(course, bootstrap != null ? bootstrap.getId() : null);
    }

    @GetMapping
    public List<CourseResponse> list() {
        return courseService.listForCurrentOrg().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{courseId}")
    public CourseDetailResponse get(@PathVariable UUID courseId) {
        Course course = courseService.get(courseId);
        List<ChapterResponse> chapters = chapterRepository.findByCourseIdOrderBySequenceAsc(courseId).stream()
                .map(ch -> ChapterResponse.from(ch, topicRepository.findByChapterIdOrderBySequenceAsc(ch.getId())))
                .toList();
        return CourseDetailResponse.from(course, chapters);
    }

    @PatchMapping("/{courseId}")
    public CourseResponse patch(@PathVariable UUID courseId, @RequestBody PatchCourseRequest request) {
        return CourseResponse.from(courseService.patch(
                courseId,
                request.title(),
                request.description(),
                request.language(),
                request.audience(),
                request.level(),
                request.durationHours()
        ));
    }

    @PostMapping("/{courseId}/generate-structure")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OperationAcceptedResponse generateStructure(@PathVariable UUID courseId) {
        AsyncOperation op = courseStructureService.generateStructureAsync(courseId);
        return new OperationAcceptedResponse(op.getId(), op.getStatus(), op.getProgress());
    }

    @PostMapping("/{courseId}/publish")
    public CourseResponse publish(
            @PathVariable UUID courseId,
            @RequestBody(required = false) PublishCourseRequest request
    ) {
        boolean allowIncomplete = request != null && Boolean.TRUE.equals(request.allowIncomplete());
        return CourseResponse.from(coursePublishService.publish(courseId, allowIncomplete));
    }

    @PatchMapping("/{courseId}/chapters/{chapterId}/topics/reorder")
    public List<TopicResponse> moveTopic(
            @PathVariable UUID courseId,
            @PathVariable UUID chapterId,
            @Valid @RequestBody MoveTopicRequest request
    ) {
        return chapterReorderService
                .moveTopicToIndex(courseId, chapterId, request.topicId(), request.targetIndex())
                .stream()
                .map(TopicResponse::from)
                .toList();
    }

    @PatchMapping("/{courseId}/chapters/reorder")
    public List<ChapterResponse> reorderChapters(
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderChaptersRequest request
    ) {
        return chapterReorderService.reorderChapters(courseId, request.chapterIds()).stream()
                .map(ch -> ChapterResponse.from(ch, topicRepository.findByChapterIdOrderBySequenceAsc(ch.getId())))
                .toList();
    }

    public record CreateCourseRequest(
            @NotBlank String title,
            String description,
            @NotNull UUID sourceMaterialId,
            @NotNull UUID sourceVersionId,
            @NotBlank String language,
            String audience,
            String level,
            BigDecimal durationHours,
            Boolean generateDrafts
    ) {
    }

    public record CourseCreateResponse(
            UUID id,
            String title,
            String status,
            String language,
            UUID bootstrapOperationId
    ) {
        static CourseCreateResponse from(Course course, UUID bootstrapOperationId) {
            return new CourseCreateResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getStatus().name(),
                    course.getLanguage(),
                    bootstrapOperationId
            );
        }
    }

    public record PatchCourseRequest(
            String title,
            String description,
            String language,
            String audience,
            String level,
            BigDecimal durationHours
    ) {
    }

    public record PublishCourseRequest(Boolean allowIncomplete) {
    }

    public record MoveTopicRequest(@NotNull UUID topicId, int targetIndex) {
    }

    public record ReorderChaptersRequest(@NotEmpty List<UUID> chapterIds) {
    }

    public record OperationAcceptedResponse(UUID operationId, OperationStatus status, int progress) {
    }

    public record CourseResponse(
            UUID id,
            UUID organizationId,
            String title,
            String description,
            UUID sourceMaterialId,
            UUID currentSourceVersionId,
            String language,
            String audience,
            String level,
            BigDecimal durationHours,
            CourseStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        static CourseResponse from(Course course) {
            return new CourseResponse(
                    course.getId(),
                    course.getOrganizationId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getSourceMaterialId(),
                    course.getCurrentSourceVersionId(),
                    course.getLanguage(),
                    course.getAudience(),
                    course.getLevel(),
                    course.getDurationHours(),
                    course.getStatus(),
                    course.getCreatedBy(),
                    course.getCreatedAt(),
                    course.getUpdatedAt()
            );
        }
    }

    public record CourseDetailResponse(
            UUID id,
            UUID organizationId,
            String title,
            String description,
            UUID sourceMaterialId,
            UUID currentSourceVersionId,
            String language,
            String audience,
            String level,
            BigDecimal durationHours,
            CourseStatus status,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            List<ChapterResponse> chapters
    ) {
        static CourseDetailResponse from(Course course, List<ChapterResponse> chapters) {
            return new CourseDetailResponse(
                    course.getId(),
                    course.getOrganizationId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getSourceMaterialId(),
                    course.getCurrentSourceVersionId(),
                    course.getLanguage(),
                    course.getAudience(),
                    course.getLevel(),
                    course.getDurationHours(),
                    course.getStatus(),
                    course.getCreatedBy(),
                    course.getCreatedAt(),
                    course.getUpdatedAt(),
                    chapters
            );
        }
    }

    public record ChapterResponse(
            UUID id,
            UUID courseId,
            String title,
            int sequence,
            StructureNodeStatus status,
            List<TopicResponse> topics
    ) {
        static ChapterResponse from(CourseChapter chapter, List<CourseTopic> topics) {
            return new ChapterResponse(
                    chapter.getId(),
                    chapter.getCourseId(),
                    chapter.getTitle(),
                    chapter.getSequence(),
                    chapter.getStatus(),
                    topics.stream().map(TopicResponse::from).toList()
            );
        }
    }

    public record TopicResponse(
            UUID id,
            UUID chapterId,
            String title,
            int sequence,
            StructureNodeStatus status
    ) {
        static TopicResponse from(CourseTopic topic) {
            return new TopicResponse(
                    topic.getId(),
                    topic.getChapterId(),
                    topic.getTitle(),
                    topic.getSequence(),
                    topic.getStatus()
            );
        }
    }
}
