package com.resqcloud.incident.service;

import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.common.exception.InvalidIncidentStateException;
import com.resqcloud.common.exception.UnauthorizedActionException;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.incident.repository.IncidentStatusHistoryRepository;
import com.resqcloud.notification.service.NotificationService;
import com.resqcloud.user.entity.User;
import com.resqcloud.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentStateTransitionServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentStatusHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    private IncidentStateTransitionService stateTransitionService;

    @BeforeEach
    void setUp() {
        stateTransitionService = new IncidentStateTransitionService(
                incidentRepository, historyRepository, userRepository, notificationService, auditLogService
        );
    }

    @Test
    @DisplayName("Should successfully transition incident from REPORTED to UNDER_REVIEW")
    void testValidTransitionReportedToUnderReview() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(incidentId);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setIncidentNumber("INC-TEST-001");

        User reporter = new User();
        reporter.setId(UUID.randomUUID());
        reporter.setUsername("reporter1");
        incident.setReporter(reporter);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident result = stateTransitionService.transitionState(incidentId, IncidentStatus.UNDER_REVIEW, "Triage initiated");

        assertNotNull(result);
        assertEquals(IncidentStatus.UNDER_REVIEW, result.getStatus());
        verify(historyRepository, times(1)).save(any());
        verify(notificationService, times(1)).notifyIncidentStatusChanged(any(), eq("REPORTED"), eq("UNDER_REVIEW"));
    }

    @Test
    @DisplayName("Should reject invalid state transition directly from REPORTED to RESOLVED")
    void testInvalidTransitionReportedToResolved() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(incidentId);
        incident.setStatus(IncidentStatus.REPORTED);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(InvalidIncidentStateException.class, () ->
                stateTransitionService.transitionState(incidentId, IncidentStatus.RESOLVED, "Illegal transition"));

        verify(incidentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject transition to the same status")
    void testSameStateTransitionRejected() {
        UUID incidentId = UUID.randomUUID();
        Incident incident = new Incident();
        incident.setId(incidentId);
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(InvalidIncidentStateException.class, () ->
                stateTransitionService.transitionState(incidentId, IncidentStatus.IN_PROGRESS, "No-op transition"));
    }
}
