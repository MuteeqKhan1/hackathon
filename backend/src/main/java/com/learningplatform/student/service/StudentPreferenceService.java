package com.learningplatform.student.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.content.domain.LearningPace;
import com.learningplatform.student.domain.StudentPreference;
import com.learningplatform.student.repository.StudentPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class StudentPreferenceService {

    private final StudentPreferenceRepository preferenceRepository;

    public StudentPreferenceService(StudentPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public StudentPreference getOrDefault() {
        AuthenticatedUser user = PermissionGuard.require(Permission.COURSE_VIEW);
        return preferenceRepository.findByStudentId(user.userId())
                .orElseGet(() -> new StudentPreference(
                        UUID.randomUUID(), user.organizationId(), user.userId(), Instant.now()
                ));
    }

    @Transactional
    public StudentPreference upsert(String language, String pace) {
        AuthenticatedUser user = PermissionGuard.require(Permission.COURSE_VIEW);
        StudentPreference pref = preferenceRepository.findByStudentId(user.userId())
                .orElseGet(() -> new StudentPreference(
                        UUID.randomUUID(), user.organizationId(), user.userId(), Instant.now()
                ));
        pref.update(language, pace);
        return preferenceRepository.save(pref);
    }

    /** Resolver helper for UT-12/13. */
    public static String resolveLanguage(String preferred, String courseLanguage) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return courseLanguage == null || courseLanguage.isBlank() ? "en" : courseLanguage;
    }

    public static LearningPace resolvePace(String preferredPace) {
        return LearningPace.fromString(preferredPace);
    }
}
