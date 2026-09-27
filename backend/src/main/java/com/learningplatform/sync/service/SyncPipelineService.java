package com.learningplatform.sync.service;

import com.learningplatform.audit.service.AuditService;
import com.learningplatform.course.domain.Course;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.course.service.CourseService;
import com.learningplatform.notification.service.NotificationService;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactLevel;
import com.learningplatform.sync.domain.ImpactStatus;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.domain.SyncEventStatus;
import com.learningplatform.sync.repository.ImpactAnalysisRepository;
import com.learningplatform.sync.repository.SyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates DETECTED → ANALYZING → IMPACT_IDENTIFIED → NOTIFIED after source publish.
 */
@Service
public class SyncPipelineService {

    private static final Logger log = LoggerFactory.getLogger(SyncPipelineService.class);

    private final SyncEventFactory syncEventFactory;
    private final SyncEventRepository syncEventRepository;
    private final ChangeDetectionService changeDetectionService;
    private final ImpactAnalyzer impactAnalyzer;
    private final ImpactAnalysisRepository impactAnalysisRepository;
    private final NotificationService notificationService;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final AuditService auditService;

    public SyncPipelineService(
            SyncEventFactory syncEventFactory,
            SyncEventRepository syncEventRepository,
            ChangeDetectionService changeDetectionService,
            ImpactAnalyzer impactAnalyzer,
            ImpactAnalysisRepository impactAnalysisRepository,
            NotificationService notificationService,
            CourseRepository courseRepository,
            CourseService courseService,
            AuditService auditService
    ) {
        this.syncEventFactory = syncEventFactory;
        this.syncEventRepository = syncEventRepository;
        this.changeDetectionService = changeDetectionService;
        this.impactAnalyzer = impactAnalyzer;
        this.impactAnalysisRepository = impactAnalysisRepository;
        this.notificationService = notificationService;
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.auditService = auditService;
    }

    @Transactional
    public SyncEvent run(UUID organizationId, UUID materialId, UUID oldVersionId, UUID newVersionId) {
        SyncEvent event = syncEventFactory.createOrGet(organizationId, materialId, oldVersionId, newVersionId);
        if (event.getStatus() != SyncEventStatus.DETECTED) {
            // Idempotent redelivery: already processed past DETECTED
            return event;
        }

        try {
            event.transitionTo(SyncEventStatus.ANALYZING);
            syncEventRepository.save(event);

            List<ChangeDetectionService.DetectedChange> changes =
                    changeDetectionService.detect(oldVersionId, newVersionId);
            List<ImpactAnalysis> impacts = impactAnalyzer.analyze(event, changes);

            event.transitionTo(SyncEventStatus.IMPACT_IDENTIFIED);
            syncEventRepository.save(event);

            notificationService.notifyInstructors(event, impacts);
            markCoursesUpdateRequired(impacts);

            event.transitionTo(SyncEventStatus.NOTIFIED);
            if (impacts.isEmpty()) {
                event.transitionTo(SyncEventStatus.COMPLETED);
            }
            syncEventRepository.save(event);
            auditService.record("SYNC_NOTIFIED", "SyncEvent", event.getId().toString(), "impacts=" + impacts.size());
            return event;
        } catch (RuntimeException ex) {
            log.error("Sync pipeline failed for event {}", event.getId(), ex);
            event.transitionTo(SyncEventStatus.FAILED);
            syncEventRepository.save(event);
            auditService.record("SYNC_FAILED", "SyncEvent", event.getId().toString(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * Idempotent consumer entry (UT-09-13): redelivery yields a single sync_event.
     */
    @Transactional
    public SyncEvent consumeSourceVersionPublished(
            UUID organizationId, UUID materialId, UUID oldVersionId, UUID newVersionId
    ) {
        return run(organizationId, materialId, oldVersionId, newVersionId);
    }

    private void markCoursesUpdateRequired(List<ImpactAnalysis> impacts) {
        Set<UUID> courseIds = new HashSet<>();
        for (ImpactAnalysis impact : impacts) {
            if (impact.getImpactLevel() == ImpactLevel.HIGH && impact.getStatus() == ImpactStatus.OPEN) {
                courseIds.add(impact.getCourseId());
            }
        }
        for (UUID courseId : courseIds) {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course != null) {
                courseService.markUpdateRequiredSystem(courseId);
            }
        }
    }
}
