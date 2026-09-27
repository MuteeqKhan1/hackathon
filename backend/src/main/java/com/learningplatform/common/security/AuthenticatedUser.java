package com.learningplatform.common.security;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        UUID organizationId,
        UserRole role,
        Set<Permission> permissions
) {
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
