package com.resqcloud.notification.service;

import com.resqcloud.incident.entity.Incident;
import com.resqcloud.notification.dto.NotificationResponse;
import com.resqcloud.notification.entity.NotificationType;
import com.resqcloud.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Core notification abstraction interface.
 * Decouples notification delivery from specific underlying transport mechanisms.
 * Currently backed by LocalNotificationService (WebSocket + database persistence);
 * designed to allow future AWS SNS / SQS implementation without changing core domain logic.
 */
public interface NotificationService {

    void notifyUser(User recipient, String title, String message, NotificationType type, Incident relatedIncident);

    void broadcast(String destinationTopic, Object payload);

    void notifyIncidentCreated(Incident incident);

    void notifyIncidentPrioritized(Incident incident);

    void notifyResponderAssigned(Incident incident, User responder);

    void notifyAssignmentStatusUpdated(Incident incident, String statusText);

    void notifyIncidentStatusChanged(Incident incident, String oldStatus, String newStatus);

    Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);

    long getUnreadCount(UUID userId);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);
}
