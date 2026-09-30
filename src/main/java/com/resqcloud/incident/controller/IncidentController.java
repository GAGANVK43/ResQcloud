package com.resqcloud.incident.controller;

import com.resqcloud.common.response.ApiResponse;
import com.resqcloud.common.response.PagedResponse;
import com.resqcloud.incident.dto.*;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.entity.IncidentType;
import com.resqcloud.incident.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Incidents", description = "Incident reporting, prioritization, lifecycle tracking, and queries")
@SecurityRequirement(name = "BearerAuth")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @Operation(summary = "Report an emergency incident", description = "Citizens or operators can report an incident with location, severity, and details")
    public ResponseEntity<ApiResponse<IncidentResponse>> reportIncident(@Valid @RequestBody IncidentCreateRequest request) {
        IncidentResponse response = incidentService.createIncident(request);
        return new ResponseEntity<>(ApiResponse.ok("Incident reported successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Search and filter incidents with pagination and sorting")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getIncidents(
            @RequestParam(required = false) IncidentType type,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) UUID reporterId,
            @RequestParam(required = false) UUID responderId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "priorityScore") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(ApiResponse.ok(incidentService.searchAndFilterIncidents(type, severity, status, reporterId, responderId, search, pageable)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get incidents reported by current user")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getMyIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getMyReportedIncidents(pageable)));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('RESPONDER', 'COORDINATOR', 'ADMIN')")
    @Operation(summary = "Get incidents assigned to current responder")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getAssignedIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getMyAssignedIncidents(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident by ID")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncidentById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getIncidentById(id)));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Get full incident details with status history and resource assignments")
    public ResponseEntity<ApiResponse<IncidentDetailsResponse>> getIncidentDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getIncidentDetails(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update incident details")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateIncident(
            @PathVariable UUID id,
            @Valid @RequestBody IncidentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Incident updated successfully", incidentService.updateIncident(id, request)));
    }

    @PostMapping("/{id}/prioritize")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    @Operation(summary = "Recalculate priority score for an incident using the priority engine")
    public ResponseEntity<ApiResponse<IncidentResponse>> prioritizeIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Priority evaluated", incidentService.recalculatePriority(id)));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Perform controlled state transition on an incident")
    public ResponseEntity<ApiResponse<IncidentResponse>> transitionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusTransitionRequest request) {
        IncidentResponse response = incidentService.transitionIncidentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Incident status updated", response));
    }
}
