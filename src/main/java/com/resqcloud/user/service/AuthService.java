package com.resqcloud.user.service;

import com.resqcloud.common.audit.AuditAction;
import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.common.exception.DuplicateResourceException;
import com.resqcloud.common.exception.ResourceNotFoundException;
import com.resqcloud.security.JwtTokenProvider;
import com.resqcloud.security.UserPrincipal;
import com.resqcloud.user.dto.AuthResponse;
import com.resqcloud.user.dto.LoginRequest;
import com.resqcloud.user.dto.RegisterRequest;
import com.resqcloud.user.dto.UserResponse;
import com.resqcloud.user.entity.ResponderProfile;
import com.resqcloud.user.entity.Role;
import com.resqcloud.user.entity.RoleType;
import com.resqcloud.user.entity.User;
import com.resqcloud.user.repository.ResponderProfileRepository;
import com.resqcloud.user.repository.RoleRepository;
import com.resqcloud.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ResponderProfileRepository responderProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       ResponderProfileRepository responderProfileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.responderProfileRepository = responderProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhoneNumber(),
                role
        );

        User savedUser = userRepository.save(user);

        // If registering as RESPONDER, create responder profile
        if (request.getRole() == RoleType.ROLE_RESPONDER) {
            String specialization = request.getSpecialization() != null ? request.getSpecialization() : "GENERAL_RESCUE";
            String badge = request.getBadgeNumber() != null ? request.getBadgeNumber() : "BADGE-" + savedUser.getId().toString().substring(0, 8).toUpperCase();
            ResponderProfile profile = new ResponderProfile(
                    savedUser,
                    specialization,
                    badge,
                    request.getLatitude() != null ? request.getLatitude() : 37.7749,
                    request.getLongitude() != null ? request.getLongitude() : -122.4194
            );
            responderProfileRepository.save(profile);
        }

        auditLogService.record(
                savedUser.getId(),
                savedUser.getUsername(),
                AuditAction.USER_REGISTERED,
                "User",
                savedUser.getId().toString(),
                null,
                "REGISTERED",
                null,
                "Role: " + role.getName().name()
        );

        return UserResponse.fromEntity(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getId()));

        auditLogService.record(
                user.getId(),
                user.getUsername(),
                AuditAction.USER_LOGIN,
                "User",
                user.getId().toString(),
                null,
                "LOGGED_IN",
                null,
                "Successful authentication"
        );

        return new AuthResponse(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getName().name()
        );
    }
}
