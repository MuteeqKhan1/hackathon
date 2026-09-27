package com.learningplatform.notification.service;

import com.learningplatform.course.domain.Course;
import com.learningplatform.course.repository.CourseRepository;
import com.learningplatform.notification.domain.Notification;
import com.learningplatform.notification.repository.NotificationRepository;
import com.learningplatform.sync.domain.ImpactAnalysis;
import com.learningplatform.sync.domain.SyncEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    public static final String TYPE_SYNC_IMPACT = "SYNC_IMPACT";

    private final NotificationRepository notificationRepository;
    private final CourseRepository courseRepository;

    public NotificationService(NotificationRepository notificationRepository, CourseRepository courseRepository) {
        this.notificationRepository = notificationRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional
    public List<Notification> notifyInstructors(SyncEvent event, List<ImpactAnalysis> impacts) {
        Set<UUID> courseIds = new HashSet<>();
        for (ImpactAnalysis impact : impacts) {
            courseIds.add(impact.getCourseId());
        }
        List<Notification> created = new java.util.ArrayList<>();
        for (UUID courseId : courseIds) {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                continue;
            }
            long existing = notificationRepository.countByRelatedEntityIdAndType(event.getId(), TYPE_SYNC_IMPACT);
            if (existing > 0 && created.stream().anyMatch(n -> n.getUserId().equals(course.getCreatedBy()))) {
                continue;
            }
            Notification n = new Notification(
                    UUID.randomUUID(),
                    event.getOrganizationId(),
                    course.getCreatedBy(),
                    TYPE_SYNC_IMPACT,
                    "Source update impacts course",
                    "Sync event " + event.getId() + " identified " + impacts.size()
                            + " impact(s) for course \"" + course.getTitle() + "\".",
                    "SyncEvent",
                    event.getId(),
                    Instant.now()
                );
            created.add(notificationRepository.save(n));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<Notification> listForUser(UUID organizationId, UUID userId) {
        return notificationRepository.findByOrganizationIdAndUserIdOrderByCreatedAtDesc(organizationId, userId);
    }
}
