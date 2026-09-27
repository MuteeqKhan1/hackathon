package com.learningplatform.student.service;

import com.learningplatform.student.domain.StudentTopicProgress;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressServiceTest {

    @Test
    void topicCompleted_100_andCourseAggregate() {
        UUID org = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID course = UUID.randomUUID();

        StudentTopicProgress t1 = new StudentTopicProgress(
                UUID.randomUUID(), org, student, course, UUID.randomUUID(), Instant.now()
        );
        t1.markLessonViewed();
        t1.markQuizCompleted();
        assertThat(t1.getPercentComplete()).isEqualTo(100);

        StudentTopicProgress t2 = new StudentTopicProgress(
                UUID.randomUUID(), org, student, course, UUID.randomUUID(), Instant.now()
        );
        t2.markLessonViewed();
        assertThat(t2.getPercentComplete()).isEqualTo(50);

        int coursePct = ProgressService.aggregateCoursePercent(List.of(t1, t2));
        assertThat(coursePct).isEqualTo(75);
    }
}
