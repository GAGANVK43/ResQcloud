package com.resqcloud.notification.service;

import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.notification.dto.NotificationResponse;
import com.resqcloud.notification.entity.Notification;
import com.resqcloud.notification.entity.NotificationType;
import com.resqcloud.notification.repository.NotificationRepository;
import com.resqcloud.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Primary
public class LocalNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LocalNotificationService(NotificationRepository notificationRepository,
                                    SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public void notifyUser(User recipient, String title, String message, NotificationType type, Incident relatedIncident) {
        if (recipient == null) return;

        Notification notification = new Notification(recipient, title, message, type, relatedIncident);
        Notification saved = notificationRepository.save(notification);
        NotificationResponse dto = NotificationResponse.fromEntity(saved);

        // Push real-time event to specific user destination
        try {
            messagingTemplate.convertAndSend("/topic/user-" + recipient.getId() + "/notifications", dto);
        } catch (Exception e) {
            log.warn("Could not dispatch WebSocket notification to user {}: {}", recipient.getId(), e.getMessage());
        }

        log.info("Notification sent to user {}: [{}] {}", recipient.getUsername(), title, message);
    }

    @Override
    public void broadcast(String destinationTopic, Object payload) {
        try {
            messagingTemplate.convertAndSend(destinationTopic, payload);
            log.debug("Broadcast event sent to topic {}", destinationTopic);
        } catch (Exception e) {
            log.warn("Failed to broadcast message to {}: {}", destinationTopic, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void notifyIncidentCreated(Incident incident) {
        // Broadcast to coordinators & responders
        broadcast("/topic/incidents", incident.getIncidentNumber() + ": " + incident.getTitle() + " reported");
        broadcast("/topic/dashboard/events", "INCIDENT_CREATED:" + incident.getId());

        // Notify reporter
        notifyUser(
                incident.getReporter(),
                "Emergency Incident Reported",
                "Your incident report #" + incident.getIncidentNumber() + " has been received and queued for review.",
                NotificationType.INCIDENT_CREATED,
                incident
        );
    }

    @Override
    @Transactional
    public void notifyIncidentPrioritized(Incident incident) {
        broadcast("/topic/dashboard/events", "INCIDENT_PRIORITIZED:" + incident.getId());
        notifyUser(
                incident.getReporter(),
                "Incident Priority Calculated",
                "Incident #" + incident.getIncidentNumber() + " evaluated with priority level: " + incident.getPriorityLevel() +
                        " (Score: " + incident.getPriorityScore() + ")",
                NotificationType.INCIDENT_PRIORITIZED,
                incident
        );
    }

    @Override
    @Transactional
    public void notifyResponderAssigned(Incident incident, User responder) {
        broadcast("/topic/dashboard/events", "RESPONDER_ASSIGNED:" + incident.getId());
        
        // Notify responder
        notifyUser(
                responder,
                "New Incident Assignment",
                "You have been assigned to incident #" + incident.getIncidentNumber() + " (" + incident.getTitle() +
                        ") at " + incident.getAddress() + ". Priority: " + incident.getPriorityLevel(),
                NotificationType.RESPONDER_ASSIGNED,
                incident
        );

        // Notify citizen
        notifyUser(
                incident.getReporter(),
                "Responder Dispatched",
                "A responder (" + responder.getFullName() + ") and emergency resources have been assigned to your incident.",
                NotificationType.RESPONDER_ASSIGNED,
                incident
        );
    }

    @Override
    @Transactional
    public void notifyAssignmentStatusUpdated(Incident incident, String statusText) {
        broadcast("/topic/dashboard/events", "ASSIGNMENT_STATUS_UPDATE:" + incident.getId());
        if (incident.getReporter() != null) {
            notifyUser(
                    incident.getReporter(),
                    "Response Update",
                    "Status update for incident #" + incident.getIncidentNumber() + ": " + statusText,
                    NotificationType.STATUS_CHANGED,
                    incident
            );
        }
    }

    @Override
    @Transactional
    public void notifyIncidentStatusChanged(Incident incident, String oldStatus, String newStatus) {
        broadcast("/topic/dashboard/events", "STATUS_CHANGED:" + incident.getId() + ":" + newStatus);
        
        String message = "Incident #" + incident.getIncidentNumber() + " status transitioned from " + oldStatus + " to " + newStatus;
        if (incident.getReporter() != null) {
            notifyUser(
                    incident.getReporter(),
                    "Incident Status Updated",
                    message,
                    NotificationType.STATUS_CHANGED,
                    incident
            );
        }
        if (incident.getAssignedResponder() != null) {
            notifyUser(
                    incident.getAssignedResponder(),
                    "Incident Status Updated",
                    message,
                    NotificationType.STATUS_CHANGED,
                    incident
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (notification.getRecipient().getId().equals(userId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForRecipient(userId);
    }
}
