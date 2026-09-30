package com.resqcloud.user.service;

import com.resqcloud.common.audit.AuditAction;
import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.common.exception.UnauthorizedActionException;
import com.resqcloud.common.response.PagedResponse;
import com.resqcloud.security.SecurityUtils;
import com.resqcloud.user.dto.ResponderStatusUpdateRequest;
import com.resqcloud.user.dto.UpdateProfileRequest;
import com.resqcloud.user.dto.UserResponse;
import com.resqcloud.user.entity.ResponderProfile;
import com.resqcloud.user.entity.RoleType;
import com.resqcloud.user.entity.User;
import com.resqcloud.user.repository.ResponderProfileRepository;
import com.resqcloud.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ResponderProfileRepository responderProfileRepository;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository,
                       ResponderProfileRepository responderProfileRepository,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.responderProfileRepository = responderProfileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedActionException("Not authenticated");
        }
        return getUserById(currentUserId);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        String oldState = String.format("name=%s, phone=%s", user.getFullName(), user.getPhoneNumber());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        User updated = userRepository.save(user);

        String newState = String.format("name=%s, phone=%s", updated.getFullName(), updated.getPhoneNumber());
        auditLogService.record(
                user.getId(),
                user.getUsername(),
                AuditAction.USER_UPDATED,
                "User",
                user.getId().toString(),
                oldState,
                newState,
                null,
                "Profile updated"
        );

        return UserResponse.fromEntity(updated);
    }

    @Transactional
    public ResponderProfile updateResponderStatus(ResponderStatusUpdateRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        ResponderProfile profile = responderProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Responder profile not found for user: " + currentUserId));

        String oldStatus = profile.getAvailabilityStatus();
        profile.setAvailabilityStatus(request.getAvailabilityStatus());
        if (request.getCurrentLatitude() != null) {
            profile.setCurrentLatitude(request.getCurrentLatitude());
        }
        if (request.getCurrentLongitude() != null) {
            profile.setCurrentLongitude(request.getCurrentLongitude());
        }

        ResponderProfile saved = responderProfileRepository.save(profile);
        auditLogService.record(
                currentUserId,
                SecurityUtils.getCurrentUsername(),
                AuditAction.USER_UPDATED,
                "ResponderProfile",
                saved.getId().toString(),
                oldStatus,
                saved.getAvailabilityStatus(),
                null,
                "Responder availability status changed"
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(RoleType roleType, String search, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(roleType, search, pageable);
        return PagedResponse.from(users.map(UserResponse::fromEntity));
    }
}
