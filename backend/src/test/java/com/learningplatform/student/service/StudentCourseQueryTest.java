package com.learningplatform.student.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.student.repository.StudentCourseEnrollmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCourseQueryTest {

    @Mock private CourseRepository courseRepository;
    @Mock private StudentCourseEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private StudentCourseQuery query;

    private final UUID org = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(user, org, UserRole.STUDENT, Permission.forRole(UserRole.STUDENT)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void draftCourse_notListed() {
        when(courseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(org, CourseStatus.PUBLISHED))
                .thenReturn(List.of());

        assertThat(query.listPublishedForCurrentStudent()).isEmpty();
    }

    @Test
    void publishedSameOrg_listed() {
        Course published = new Course(
                UUID.randomUUID(), org, "Physics", null, UUID.randomUUID(), UUID.randomUUID(),
                "en", null, null, BigDecimal.ONE, CourseStatus.PUBLISHED, user, Instant.now(), Instant.now()
        );
        when(courseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(org, CourseStatus.PUBLISHED))
                .thenReturn(List.of(published));

        assertThat(query.listPublishedForCurrentStudent()).containsExactly(published);
    }
}
