package com.learningplatform.notification.service;

import com.learningplatform.course.domain.Course;
import com.learningplatform.course.domain.CourseStatus;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.notification.domain.Notification;
import com.learningplatform.notification.repository.NotificationRepository;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.ImpactLevel;
import com.learningplatform.sync.domain.ImpactStatus;
import com.learningplatform.sync.domain.RecommendedAction;
import com.learningplatform.sync.domain.SyncEvent;
import com.learningplatform.sync.domain.SyncEventStatus;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private CourseRepository courseRepository;

    @InjectMocks
    private NotificationService service;

    @Test
    void impactsIdentified_createsInAppNotification() {
        UUID org = UUID.randomUUID();
        UUID instructor = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        SyncEvent event = new SyncEvent(
                eventId, org, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                SyncEventStatus.IMPACT_IDENTIFIED, "k", Instant.now()
        );
        ImpactAnalysis impact = new ImpactAnalysis(
                UUID.randomUUID(), eventId, courseId, UUID.randomUUID(), UUID.randomUUID(),
                ImpactLevel.HIGH, "r", RecommendedAction.REGENERATE, "s", ImpactStatus.OPEN, Instant.now()
        );
        Course course = new Course(
                courseId, org, "Physics", null, UUID.randomUUID(), UUID.randomUUID(), "en",
                null, null, BigDecimal.ONE, CourseStatus.PUBLISHED, instructor, Instant.now(), Instant.now()
        );

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(notificationRepository.countByRelatedEntityIdAndType(eventId, NotificationService.TYPE_SYNC_IMPACT))
                .thenReturn(0L);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Notification> notes = service.notifyInstructors(event, List.of(impact));

        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getUserId()).isEqualTo(instructor);
        assertThat(notes.get(0).getType()).isEqualTo(NotificationService.TYPE_SYNC_IMPACT);
    }
}
