package com.learningplatform.sync.service;

import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.course.service.CourseService;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactLevel;
import com.learningplatform.sync.domain.ImpactStatus;
import com.learningplatform.sync.domain.RecommendedAction;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.domain.SyncEventStatus;
import com.learningplatform.sync.repository.ImpactAnalysisRepository;
import com.learningplatform.sync.repository.SyncEventRepository;
import com.learningplatform.audit.service.AuditService;
import com.learningplatform.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncPipelineServiceTest {

    @Mock private SyncEventFactory syncEventFactory;
    @Mock private SyncEventRepository syncEventRepository;
    @Mock private ChangeDetectionService changeDetectionService;
    @Mock private ImpactAnalyzer impactAnalyzer;
    @Mock private ImpactAnalysisRepository impactAnalysisRepository;
    @Mock private NotificationService notificationService;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseService courseService;
    @Mock private AuditService auditService;

    @InjectMocks
    private SyncPipelineService pipeline;

    @Test
    void openHighImpacts_markCourseUpdateRequired() {
        UUID org = UUID.randomUUID();
        UUID material = UUID.randomUUID();
        UUID oldV = UUID.randomUUID();
        UUID newV = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        SyncEvent event = new SyncEvent(
                UUID.randomUUID(), org, material, oldV, newV, SyncEventStatus.DETECTED, "k", Instant.now()
        );
        ImpactAnalysis high = new ImpactAnalysis(
                UUID.randomUUID(), event.getId(), courseId, UUID.randomUUID(), UUID.randomUUID(),
                ImpactLevel.HIGH, "r", RecommendedAction.REGENERATE, "s", ImpactStatus.OPEN, Instant.now()
        );
        Course course = new Course(
                courseId, org, "C", null, material, oldV, "en", null, null, BigDecimal.ONE,
                CourseStatus.PUBLISHED, UUID.randomUUID(), Instant.now(), Instant.now()
        );

        when(syncEventFactory.createOrGet(org, material, oldV, newV)).thenReturn(event);
        when(syncEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(changeDetectionService.detect(oldV, newV)).thenReturn(List.of());
        when(impactAnalyzer.analyze(eq(event), any())).thenReturn(List.of(high));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        SyncEvent result = pipeline.run(org, material, oldV, newV);

        assertThat(result.getStatus()).isEqualTo(SyncEventStatus.NOTIFIED);
        verify(courseService).markUpdateRequiredSystem(courseId);
    }

    @Test
    void idempotentConsumer_redelivery_singleEvent() {
        UUID org = UUID.randomUUID();
        UUID material = UUID.randomUUID();
        UUID oldV = UUID.randomUUID();
        UUID newV = UUID.randomUUID();
        SyncEvent existing = new SyncEvent(
                UUID.randomUUID(), org, material, oldV, newV, SyncEventStatus.NOTIFIED, "k", Instant.now()
        );

        when(syncEventFactory.createOrGet(org, material, oldV, newV)).thenReturn(existing);

        SyncEvent first = pipeline.consumeSourceVersionPublished(org, material, oldV, newV);
        SyncEvent second = pipeline.consumeSourceVersionPublished(org, material, oldV, newV);

        assertThat(first.getId()).isEqualTo(existing.getId());
        assertThat(second.getId()).isEqualTo(existing.getId());
        verify(changeDetectionService, never()).detect(any(), any());
        verify(syncEventFactory, times(2)).createOrGet(org, material, oldV, newV);
    }
}
