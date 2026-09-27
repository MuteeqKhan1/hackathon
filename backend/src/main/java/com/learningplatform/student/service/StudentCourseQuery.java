package com.learningplatform.student.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.student.domain.StudentCourseEnrollment;
import com.learningplatform.student.repository.StudentCourseEnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StudentCourseQuery {

    private final CourseRepository courseRepository;
    private final StudentCourseEnrollmentRepository enrollmentRepository;

    public StudentCourseQuery(
            CourseRepository courseRepository,
            StudentCourseEnrollmentRepository enrollmentRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> listPublishedForCurrentStudent() {
        AuthenticatedUser user = PermissionGuard.require(Permission.COURSE_VIEW);
        return courseRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                user.organizationId(),
                CourseStatus.PUBLISHED
        );
    }

    @Transactional
    public Course getPublished(UUID courseId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.COURSE_VIEW);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("COURSE_NOT_FOUND", "Course was not found."));
        if (!course.getOrganizationId().equals(user.organizationId())) {
            throw new NotFoundException("COURSE_NOT_FOUND", "Course was not found.");
        }
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new NotFoundException("COURSE_NOT_PUBLISHED", "Course is not available to students.");
        }
        ensureEnrolled(user, course);
        return course;
    }

    private void ensureEnrolled(AuthenticatedUser user, Course course) {
        if (!enrollmentRepository.existsByStudentIdAndCourseId(user.userId(), course.getId())) {
            enrollmentRepository.save(new StudentCourseEnrollment(
                    UUID.randomUUID(),
                    user.organizationId(),
                    user.userId(),
                    course.getId(),
                    Instant.now()
            ));
        }
    }
}
