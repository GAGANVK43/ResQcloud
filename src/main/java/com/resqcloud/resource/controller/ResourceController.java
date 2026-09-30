package com.resqcloud.resource.controller;

import com.resqcloud.common.response.ApiResponse;
import com.resqcloud.common.response.PagedResponse;
import com.resqcloud.resource.dto.*;
import com.resqcloud.resource.entity.AssignmentStatus;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.entity.ResourceType;
import com.resqcloud.resource.service.ResourceMatchingService;
import com.resqcloud.resource.service.ResourceService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
@Tag(name = "Resources", description = "Emergency resource inventory, intelligent matching, and assignment")
@SecurityRequirement(name = "BearerAuth")
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceMatchingService matchingService;

    public ResourceController(ResourceService resourceService, ResourceMatchingService matchingService) {
        this.resourceService = resourceService;
        this.matchingService = matchingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Register new emergency resource", description = "Admin or Coordinator only")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(@Valid @RequestBody ResourceCreateRequest request) {
        ResourceResponse response = resourceService.createResource(request);
        return new ResponseEntity<>(ApiResponse.ok("Resource created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Update emergency resource details or status")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable UUID id,
            @Valid @RequestBody ResourceUpdateRequest request) {
        ResourceResponse response = resourceService.updateResource(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Resource updated successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource details by ID")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getResourceById(id)));
    }

    @GetMapping
    @Operation(summary = "List resources with search, filtering, and pagination")
    public ResponseEntity<ApiResponse<PagedResponse<ResourceResponse>>> listResources(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) ResourceStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.ok(resourceService.listResources(type, status, search, pageable)));
    }

    @GetMapping("/available")
    @Operation(summary = "Get all currently available emergency resources")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> getAvailableResources() {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getAvailableResources()));
    }

    @GetMapping("/matching/{incidentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get intelligent resource recommendations for an incident",
            description = "Calculates composite match based on distance, capability match, and workload")
    public ResponseEntity<ApiResponse<List<ResourceMatchRecommendation>>> getRecommendations(
            @PathVariable UUID incidentId,
            @RequestParam(defaultValue = "5") int limit) {
        List<ResourceMatchRecommendation> recommendations = matchingService.getRecommendationsForIncident(incidentId, limit);
        return ResponseEntity.ok(ApiResponse.ok(recommendations));
    }

    @PostMapping("/{incidentId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Assign a resource and/or responder to an incident")
    public ResponseEntity<ApiResponse<ResourceAssignmentResponse>> assignResource(
            @PathVariable UUID incidentId,
            @Valid @RequestBody ResourceAssignmentRequest request) {
        ResourceAssignmentResponse response = resourceService.assignResourceToIncident(incidentId, request);
        return ResponseEntity.ok(ApiResponse.ok("Resource assigned successfully", response));
    }

    @PatchMapping("/assignments/{assignmentId}/status")
    @Operation(summary = "Update assignment status (e.g. ACCEPTED, EN_ROUTE, ON_SCENE, COMPLETED, RELEASED)")
    public ResponseEntity<ApiResponse<ResourceAssignmentResponse>> updateAssignmentStatus(
            @PathVariable UUID assignmentId,
            @RequestParam AssignmentStatus status) {
        ResourceAssignmentResponse response = resourceService.updateAssignmentStatus(assignmentId, status);
        return ResponseEntity.ok(ApiResponse.ok("Assignment status updated", response));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Release a resource back to the available pool")
    public ResponseEntity<ApiResponse<String>> releaseResource(@PathVariable UUID id) {
        resourceService.releaseResource(id);
        return ResponseEntity.ok(ApiResponse.ok("Resource released successfully", "SUCCESS"));
    }

    @GetMapping("/incident/{incidentId}/assignments")
    @Operation(summary = "Get all resource assignments for a specific incident")
    public ResponseEntity<ApiResponse<List<ResourceAssignmentResponse>>> getAssignmentsForIncident(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.getAssignmentsForIncident(incidentId)));
    }
}
