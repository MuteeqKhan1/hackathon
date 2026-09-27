package com.learningplatform.course.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.exception.ForbiddenException;
import com.learningplatform.common.exception.NotFoundException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import com.learningplatform.source.repository.SourceMaterialRepository;
import com.learningplatform.source.repository.SourceMaterialVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final SourceMaterialRepository sourceMaterialRepository;
    private final SourceMaterialVersionRepository sourceMaterialVersionRepository;
    private final AuditService auditService;

    public CourseService(
            CourseRepository courseRepository,
            SourceMaterialRepository sourceMaterialRepository,
            SourceMaterialVersionRepository sourceMaterialVersionRepository,
            AuditService auditService
    ) {
        this.courseRepository = courseRepository;
        this.sourceMaterialRepository = sourceMaterialRepository;
        this.sourceMaterialVersionRepository = sourceMaterialVersionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Course create(
            String title,
            String description,
            UUID sourceMaterialId,
            UUID sourceVersionId,
            String language,
            String audience,
            String level,
            BigDecimal durationHours
    ) {
        AuthenticatedUser user = PermissionGuard.require(Permission.COURSE_CREATE);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language must not be blank");
        }

        SourceMaterial material = sourceMaterialRepository.findById(sourceMaterialId)
                .orElseThrow(() -> new NotFoundException("SOURCE_MATERIAL_NOT_FOUND", "Source material was not found."));
        if (!material.getOrganizationId().equals(user.organizationId())) {
            throw new ForbiddenException("Cross-organization access denied. Cannot bind course to another org's source.");
        }

        SourceMaterialVersion version = sourceMaterialVersionRepository
                .findByIdAndSourceMaterialId(sourceVersionId, sourceMaterialId)
                .orElseThrow(() -> new NotFoundException("SOURCE_VERSION_NOT_FOUND", "Source version was not found for material."));
        if (version.getStatus() != SourceVersionStatus.PUBLISHED) {
            throw new BusinessException("SOURCE_VERSION_NOT_PUBLISHED", "Course must bind to a PUBLISHED source version.");
        }

        Instant now = Instant.now();
        Course course = new Course(
                UUID.randomUUID(),
                user.organizationId(),
                title.trim(),
                description,
                sourceMaterialId,
                sourceVersionId,
                language.trim(),
                audience,
                level,
                durationHours,
                CourseStatus.DRAFT,
                user.userId(),
                now,
                now
        );
        courseRepository.save(course);
        auditService.record("COURSE_CREATED", "Course", course.getId().toString(), course.getTitle());
        return course;
    }

    @Transactional(readOnly = true)
    public Course get(UUID courseId) {
        PermissionGuard.require(Permission.COURSE_VIEW);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("COURSE_NOT_FOUND", "Course was not found."));
        PermissionGuard.requireSameOrganization(course.getOrganizationId());
        return course;
    }

    @Transactional(readOnly = true)
    public List<Course> listForCurrentOrg() {
        AuthenticatedUser user = PermissionGuard.require(Permission.COURSE_VIEW);
        return courseRepository.findByOrganizationIdOrderByCreatedAtDesc(user.organizationId());
    }

    @Transactional
    public Course patch(
            UUID courseId,
            String title,
            String description,
            String language,
            String audience,
            String level,
            BigDecimal durationHours
    ) {
        PermissionGuard.require(Permission.COURSE_EDIT);
        Course course = get(courseId);
        course.patch(title, description, language, audience, level, durationHours);
        courseRepository.save(course);
        auditService.record("COURSE_UPDATED", "Course", course.getId().toString(), course.getTitle());
        return course;
    }

    @Transactional
    public Course markUpdateRequired(UUID courseId) {
        PermissionGuard.require(Permission.COURSE_EDIT);
        Course course = get(courseId);
        course.markUpdateRequired();
        courseRepository.save(course);
        auditService.record("COURSE_UPDATE_REQUIRED", "Course", course.getId().toString(), course.getStatus().name());
        return course;
    }

    /** System path for sync pipeline (no instructor session required). */
    @Transactional
    public Course markUpdateRequiredSystem(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("COURSE_NOT_FOUND", "Course was not found."));
        if (course.getStatus() == CourseStatus.UPDATE_REQUIRED) {
            return course;
        }
        course.markUpdateRequired();
        courseRepository.save(course);
        auditService.record("COURSE_UPDATE_REQUIRED", "Course", course.getId().toString(), course.getStatus().name());
        return course;
    }
}
