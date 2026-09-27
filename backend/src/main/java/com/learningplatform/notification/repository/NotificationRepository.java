package com.learningplatform.notification.repository;

import com.learningplatform.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, UUID userId);

    long countByRelatedEntityIdAndType(UUID relatedEntityId, String type);
}
