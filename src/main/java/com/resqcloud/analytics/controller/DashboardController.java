package com.resqcloud.analytics.controller;

import com.resqcloud.analytics.dto.DashboardSummaryResponse;
import com.resqcloud.analytics.service.AnalyticsService;
import com.resqcloud.common.response.ApiResponse;
import com.resqcloud.incident.dto.IncidentResponse;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.notification.repository.NotificationRepository;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.repository.ResourceAssignmentRepository;
import com.resqcloud.resource.repository.ResourceRepository;
import com.resqcloud.security.SecurityUtils;
import com.resqcloud.user.entity.ResponderProfile;
import com.resqcloud.user.repository.ResponderProfileRepository;
import com.resqcloud.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboards", description = "Role-specific operational and analytical dashboards")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final AnalyticsService analyticsService;
    private final IncidentRepository incidentRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceAssignmentRepository assignmentRepository;
    private final ResponderProfileRepository responderProfileRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public DashboardController(AnalyticsService analyticsService,
                               IncidentRepository incidentRepository,
                               ResourceRepository resourceRepository,
                               ResourceAssignmentRepository assignmentRepository,
                               ResponderProfileRepository responderProfileRepository,
                               NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.incidentRepository = incidentRepository;
        this.resourceRepository = resourceRepository;
        this.assignmentRepository = assignmentRepository;
        this.responderProfileRepository = responderProfileRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get general dashboard summary metrics")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getDashboardSummary()));
    }

    @GetMapping("/coordinator")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    @Operation(summary = "Coordinator operations dashboard data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCoordinatorDashboard() {
        Map<String, Object> data = new HashMap<>();
        DashboardSummaryResponse summary = analyticsService.getDashboardSummary();
        data.put("metrics", summary);

        // Fetch top 10 prioritized active incidents queue
        Pageable queuePage = PageRequest.of(0, 10, Sort.by("priorityScore").descending());
        var activeIncidents = incidentRepository.searchAndFilterIncidents(
                null, null, null, null, null, null, queuePage
        ).map(IncidentResponse::fromEntity);
        data.put("activeQueue", activeIncidents.getContent());

        // Available resources count
        data.put("availableResourcesCount", resourceRepository.countByStatus(ResourceStatus.AVAILABLE));

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/responder")
    @PreAuthorize("hasRole('RESPONDER')")
    @Operation(summary = "Responder personal assignments and profile dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getResponderDashboard() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Map<String, Object> data = new HashMap<>();

        // Profile
        ResponderProfile profile = responderProfileRepository.findByUserId(currentUserId).orElse(null);
        data.put("profile", profile);

        // Active assignments
        var assignments = assignmentRepository.findActiveAssignmentsForResponder(currentUserId);
        data.put("activeAssignmentsCount", assignments.size());

        // Assigned incidents
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        var assignedIncidents = incidentRepository.findByAssignedResponderIdOrderByCreatedAtDesc(currentUserId, pageable)
                .map(IncidentResponse::fromEntity);
        data.put("assignedIncidents", assignedIncidents.getContent());

        // Unread notifications
        data.put("unreadNotifications", notificationRepository.countByRecipientIdAndReadFalse(currentUserId));

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/citizen")
    @Operation(summary = "Citizen personal incident tracking dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCitizenDashboard() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Map<String, Object> data = new HashMap<>();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        var myIncidents = incidentRepository.findByReporterIdOrderByCreatedAtDesc(currentUserId, pageable)
                .map(IncidentResponse::fromEntity);
        data.put("myIncidents", myIncidents.getContent());
        data.put("totalReported", incidentRepository.findByReporterIdOrderByCreatedAtDesc(currentUserId, Pageable.unpaged()).getTotalElements());
        data.put("unreadNotifications", notificationRepository.countByRecipientIdAndReadFalse(currentUserId));

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin overview dashboard with system health and statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("metrics", analyticsService.getDashboardSummary());
        data.put("totalUsers", userRepository.count());
        data.put("totalResources", resourceRepository.count());
        data.put("totalIncidents", incidentRepository.count());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
