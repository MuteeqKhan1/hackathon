package com.learningplatform.common.security;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.exception.ForbiddenException;

import java.util.UUID;

public final class PermissionGuard {

    private PermissionGuard() {
    }

    public static AuthenticatedUser require(Permission permission) {
        AuthenticatedUser user = SecurityContext.require();
        if (!user.hasPermission(permission)) {
            throw new ForbiddenException(
                    "Missing permission: " + permission.name()
                            + " for role " + user.role().name()
                            + ". Course authoring needs INSTRUCTOR; source upload/publish needs CONTENT_OWNER."
            );
        }
        return user;
    }

    public static void requireSameOrganization(UUID resourceOrganizationId) {
        AuthenticatedUser user = SecurityContext.require();
        if (!user.organizationId().equals(resourceOrganizationId)) {
            throw new ForbiddenException(
                    "Cross-organization access denied. Active org="
                            + user.organizationId()
                            + " resource org="
                            + resourceOrganizationId
                            + ". Open Organizations and click the org that owns this material."
            );
        }
    }
}