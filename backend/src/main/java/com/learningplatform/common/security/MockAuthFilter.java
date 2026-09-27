package com.learningplatform.common.security;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * v1 mock auth: X-User-Id, X-Org-Id, X-Role.
 * Replaced by OIDC later without changing PermissionGuard call sites.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MockAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ORG_ID = "X-Org-Id";
    public static final String HEADER_ROLE = "X-Role";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userIdHeader = request.getHeader(HEADER_USER_ID);
            String orgIdHeader = request.getHeader(HEADER_ORG_ID);
            String roleHeader = request.getHeader(HEADER_ROLE);

            if (isBlank(userIdHeader) || isBlank(orgIdHeader) || isBlank(roleHeader)) {
                writeUnauthorized(response, "Missing required auth headers: X-User-Id, X-Org-Id, X-Role");
                return;
            }

            UUID userId;
            UUID orgId;
            try {
                userId = UUID.fromString(userIdHeader.trim());
                orgId = UUID.fromString(orgIdHeader.trim());
            } catch (IllegalArgumentException ex) {
                writeUnauthorized(response, "Invalid UUID in X-User-Id or X-Org-Id");
                return;
            }

            UserRole role;
            try {
                role = UserRole.valueOf(roleHeader.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                writeUnauthorized(response, "Invalid X-Role. Expected CONTENT_OWNER, INSTRUCTOR, or STUDENT");
                return;
            }

            Set<Permission> permissions = Permission.forRole(role);
            SecurityContext.set(new AuthenticatedUser(userId, orgId, role, permissions));
            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message.replace("\"", "'") + "\",\"correlationId\":\"unknown\"}"
        );
    }
}
