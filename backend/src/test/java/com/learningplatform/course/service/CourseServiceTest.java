package com.learningplatform.course.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.exception.ForbiddenException;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.source.domain.SourceMaterial;
import com.learningplatform.source.domain.SourceMaterialStatus;
import com.learningplatform.source.domain.SourceMaterialVersion;
import com.learningplatform.source.domain.SourceVersionStatus;
import com.learningplatform.source.repository.SourceMaterialRepository;
import com.learningplatform.source.repository.SourceMaterialVersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final UUID ORG_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MATERIAL = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VERSION = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SourceMaterialRepository sourceMaterialRepository;
    @Mock
    private SourceMaterialVersionRepository sourceMaterialVersionRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private CourseService courseService;

    @BeforeEach
    void auth() {
        SecurityContext.set(new AuthenticatedUser(USER, ORG_A, UserRole.INSTRUCTOR, Permission.forRole(UserRole.INSTRUCTOR)));
    }

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void create_sourceInSameOrg_courseDraftBoundToVersion() {
        when(sourceMaterialRepository.findById(MATERIAL)).thenReturn(Optional.of(material(ORG_A)));
        when(sourceMaterialVersionRepository.findByIdAndSourceMaterialId(VERSION, MATERIAL))
                .thenReturn(Optional.of(publishedVersion()));
        when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Course course = courseService.create(
                "Physics 10",
                "desc",
                MATERIAL,
                VERSION,
                "en",
                "Grade 10",
                "Beginner",
                BigDecimal.valueOf(20)
        );

        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(course.getOrganizationId()).isEqualTo(ORG_A);
        assertThat(course.getSourceMaterialId()).isEqualTo(MATERIAL);
        assertThat(course.getCurrentSourceVersionId()).isEqualTo(VERSION);
    }

    @Test
    void create_sourceOtherOrg_deny() {
        when(sourceMaterialRepository.findById(MATERIAL)).thenReturn(Optional.of(material(ORG_B)));

        assertThatThrownBy(() -> courseService.create(
                "Physics 10", null, MATERIAL, VERSION, "en", null, null, null
        )).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void markUpdateRequired_setsStatus() {
        Course existing = new Course(
                UUID.randomUUID(), ORG_A, "Physics 10", null, MATERIAL, VERSION,
                "en", null, null, null, CourseStatus.DRAFT, USER, Instant.now(), Instant.now()
        );
        when(courseRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(courseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Course updated = courseService.markUpdateRequired(existing.getId());

        assertThat(updated.getStatus()).isEqualTo(CourseStatus.UPDATE_REQUIRED);
    }

    private static SourceMaterial material(UUID orgId) {
        return new SourceMaterial(MATERIAL, orgId, "Physics", null, "Physics",
                SourceMaterialStatus.ACTIVE, USER, Instant.now());
    }

    private static SourceMaterialVersion publishedVersion() {
        return new SourceMaterialVersion(
                VERSION, MATERIAL, 1, "key", "f.pdf", "application/pdf", 10L, "hash",
                SourceVersionStatus.PUBLISHED, USER, Instant.now(), Instant.now()
        );
    }
}
