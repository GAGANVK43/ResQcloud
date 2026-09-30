package com.resqcloud.user.controller;

import com.resqcloud.common.response.ApiResponse;
import com.resqcloud.common.response.PagedResponse;
import com.resqcloud.user.dto.ResponderStatusUpdateRequest;
import com.resqcloud.user.dto.UpdateProfileRequest;
import com.resqcloud.user.dto.UserResponse;
import com.resqcloud.user.entity.ResponderProfile;
import com.resqcloud.user.entity.RoleType;
import com.resqcloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile and responder status management")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getCurrentUserProfile()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", userService.updateProfile(request)));
    }

    @PostMapping("/responder/status")
    @PreAuthorize("hasRole('RESPONDER')")
    @Operation(summary = "Update responder availability and GPS location", description = "For responders to toggle AVAILABLE, BUSY, or OFFLINE")
    public ResponseEntity<ApiResponse<ResponderProfile>> updateResponderStatus(@Valid @RequestBody ResponderStatusUpdateRequest request) {
        ResponderProfile profile = userService.updateResponderStatus(request);
        return ResponseEntity.ok(ApiResponse.ok("Status updated successfully", profile));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users with filtering and pagination", description = "Admin only")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> listUsers(
            @RequestParam(required = false) RoleType role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers(role, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }
}
