package com.learningplatform.common.api;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.domain.UserRole;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class MeController {

    @GetMapping("/me")
    public MeResponse me() {
        AuthenticatedUser user = SecurityContext.require();
        return new MeResponse(
                user.userId(),
                user.organizationId(),
                user.role(),
                user.permissions().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }

    public record MeResponse(
            UUID userId,
            UUID organizationId,
            UserRole role,
            Set<String> permissions
    ) {
    }
}
