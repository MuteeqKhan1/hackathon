package com.learningplatform.student.service;

import com.learningplatform.common.domain.Permission;
import com.learningplatform.common.security.AuthenticatedUser;
import com.learningplatform.common.security.PermissionGuard;
import com.learningplatform.student.domain.StudentMastery;
import com.learningplatform.student.repository.StudentMasteryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class MasteryService {

    private final StudentMasteryRepository masteryRepository;

    public MasteryService(StudentMasteryRepository masteryRepository) {
        this.masteryRepository = masteryRepository;
    }

    @Transactional
    public StudentMastery recordAttempt(UUID topicId, BigDecimal quizScore) {
        AuthenticatedUser user = PermissionGuard.require(Permission.PROGRESS_VIEW);
        StudentMastery mastery = masteryRepository.findByStudentIdAndTopicId(user.userId(), topicId)
                .orElseGet(() -> new StudentMastery(
                        UUID.randomUUID(),
                        user.organizationId(),
                        user.userId(),
                        topicId,
                        Instant.now()
                ));
        mastery.recordAttempt(quizScore);
        return masteryRepository.save(mastery);
    }

    @Transactional(readOnly = true)
    public StudentMastery get(UUID topicId) {
        AuthenticatedUser user = PermissionGuard.require(Permission.PROGRESS_VIEW);
        return masteryRepository.findByStudentIdAndTopicId(user.userId(), topicId).orElse(null);
    }
}
