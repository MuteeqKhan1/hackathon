package com.learningplatform.common.security;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionGuardTest {

    private static final UUID ORG_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORG_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @AfterEach
    void clear() {
        SecurityContext.clear();
    }

    @Test
    void require_instructorWithCourseCreate_allows() {
        SecurityContext.set(user(UserRole.INSTRUCTOR, ORG_A));
        AuthenticatedUser result = PermissionGuard.require(Permission.COURSE_CREATE);
        assertThat(result.role()).isEqualTo(UserRole.INSTRUCTOR);
    }

    @Test
    void require_studentWithCourseCreate_denies() {
        SecurityContext.set(user(UserRole.STUDENT, ORG_A));
        assertThatThrownBy(() -> PermissionGuard.require(Permission.COURSE_CREATE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("COURSE_CREATE");
    }

    @Test
    void require_contentOwnerWithSourcePublish_allows() {
        SecurityContext.set(user(UserRole.CONTENT_OWNER, ORG_A));
        assertThat(PermissionGuard.require(Permission.SOURCE_PUBLISH).organizationId()).isEqualTo(ORG_A);
    }

    @Test
    void require_instructorWithSourcePublish_denies() {
        SecurityContext.set(user(UserRole.INSTRUCTOR, ORG_A));
        assertThatThrownBy(() -> PermissionGuard.require(Permission.SOURCE_PUBLISH))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireSameOrganization_otherOrg_denies() {
        SecurityContext.set(user(UserRole.INSTRUCTOR, ORG_A));
        assertThatThrownBy(() -> PermissionGuard.requireSameOrganization(ORG_B))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Cross-organization");
    }

    @Test
    void requireSameOrganization_sameOrg_allows() {
        SecurityContext.set(user(UserRole.INSTRUCTOR, ORG_A));
        PermissionGuard.requireSameOrganization(ORG_A);
    }

    private static AuthenticatedUser user(UserRole role, UUID orgId) {
        return new AuthenticatedUser(USER, orgId, role, Permission.forRole(role));
    }
}
