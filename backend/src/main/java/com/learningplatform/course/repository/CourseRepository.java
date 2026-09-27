package com.learningplatform.course.repository;

import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<Course> findBySourceMaterialId(UUID sourceMaterialId);

    List<Course> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, CourseStatus status);
}
