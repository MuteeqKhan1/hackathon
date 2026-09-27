package com.learningplatform.student.api;

import com.learningplatform.course.domain.Course;
import com.learningplatform.student.domain.StudentPreference;
import com.learningplatform.student.domain.StudentTopicProgress;
import com.learningplatform.student.service.ProgressService;
import com.learningplatform.student.service.StudentLearningService;
import com.learningplatform.student.service.StudentPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
public class StudentController {

    private final StudentLearningService learningService;
    private final ProgressService progressService;
    private final StudentPreferenceService preferenceService;

    public StudentController(
            StudentLearningService learningService,
            ProgressService progressService,
            StudentPreferenceService preferenceService
    ) {
        this.learningService = learningService;
        this.progressService = progressService;
        this.preferenceService = preferenceService;
    }

    @GetMapping("/preferences")
    public PreferenceView preferences() {
        return PreferenceView.from(preferenceService.getOrDefault());
    }

    @PutMapping("/preferences")
    public PreferenceView updatePreferences(@RequestBody PreferenceBody body) {
        return PreferenceView.from(preferenceService.upsert(body.preferredLanguage(), body.preferredPace()));
    }

    @GetMapping("/courses")
    public List<CourseSummary> courses() {
        return learningService.listCourses().stream().map(CourseSummary::from).toList();
    }

    @GetMapping("/courses/{courseId}")
    public CourseDetail detail(@PathVariable UUID courseId) {
        StudentLearningService.StudentCourseDetail d = learningService.courseDetail(courseId);
        return CourseDetail.from(d);
    }

    @GetMapping("/topics/{topicId}")
    public StudentLearningService.TopicLessonView topic(@PathVariable UUID topicId) {
        return learningService.topicLesson(topicId);
    }

    @GetMapping("/topics/{topicId}/video.mp4")
    public ResponseEntity<byte[]> topicVideo(@PathVariable UUID topicId) {
        byte[] mp4 = learningService.topicVideoMp4(topicId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"lesson.mp4\"")
                .contentType(MediaType.valueOf("video/mp4"))
                .contentLength(mp4.length)
                .body(mp4);
    }

    @PostMapping("/quizzes/{quizId}/attempts")
    public StudentLearningService.AttemptResult attempt(
            @PathVariable UUID quizId,
            @Valid @RequestBody AttemptBody body
    ) {
        return learningService.submitAttempt(quizId, body.answers());
    }

    @GetMapping("/progress/{courseId}")
    public ProgressView progress(@PathVariable UUID courseId) {
        ProgressService.CourseProgressView view = progressService.courseProgress(courseId);
        return new ProgressView(
                view.courseId(),
                view.coursePercent(),
                view.topics().stream().map(TopicProgressView::from).toList()
        );
    }

    public record AttemptBody(@NotNull List<StudentLearningService.AnswerSubmission> answers) {
    }

    public record PreferenceBody(String preferredLanguage, String preferredPace) {
    }

    public record PreferenceView(String preferredLanguage, String preferredPace) {
        static PreferenceView from(StudentPreference p) {
            return new PreferenceView(p.getPreferredLanguage(), p.getPreferredPace());
        }
    }

    public record CourseSummary(UUID id, String title, String language, String status) {
        static CourseSummary from(Course c) {
            return new CourseSummary(c.getId(), c.getTitle(), c.getLanguage(), c.getStatus().name());
        }
    }

    public record CourseDetail(
            UUID id,
            String title,
            String language,
            String status,
            List<StudentLearningService.ChapterNode> chapters
    ) {
        static CourseDetail from(StudentLearningService.StudentCourseDetail d) {
            return new CourseDetail(
                    d.course().getId(),
                    d.course().getTitle(),
                    d.course().getLanguage(),
                    d.course().getStatus().name(),
                    d.chapters()
            );
        }
    }

    public record ProgressView(UUID courseId, int coursePercent, List<TopicProgressView> topics) {
    }

    public record TopicProgressView(
            UUID topicId,
            boolean lessonViewed,
            boolean quizCompleted,
            int percentComplete
    ) {
        static TopicProgressView from(StudentTopicProgress p) {
            return new TopicProgressView(
                    p.getTopicId(),
                    p.isLessonViewed(),
                    p.isQuizCompleted(),
                    p.getPercentComplete()
            );
        }
    }
}
