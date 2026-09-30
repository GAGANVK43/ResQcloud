package com.resqcloud.seeder;

import com.resqcloud.common.audit.AuditAction;
import com.resqcloud.common.audit.AuditLogService;
import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.entity.IncidentType;
import com.resqcloud.incident.repository.IncidentRepository;
import com.resqcloud.incident.service.IncidentPriorityService;
import com.resqcloud.notification.entity.NotificationType;
import com.resqcloud.notification.service.NotificationService;
import com.resqcloud.resource.entity.AssignmentStatus;
import com.resqcloud.resource.entity.Resource;
import com.resqcloud.resource.entity.ResourceAssignment;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.entity.ResourceType;
import com.resqcloud.resource.repository.ResourceAssignmentRepository;
import com.resqcloud.resource.repository.ResourceRepository;
import com.resqcloud.user.entity.ResponderProfile;
import com.resqcloud.user.entity.Role;
import com.resqcloud.user.entity.RoleType;
import com.resqcloud.user.entity.User;
import com.resqcloud.user.repository.ResponderProfileRepository;
import com.resqcloud.user.repository.RoleRepository;
import com.resqcloud.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ResponderProfileRepository responderProfileRepository;
    private final ResourceRepository resourceRepository;
    private final IncidentRepository incidentRepository;
    private final ResourceAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final IncidentPriorityService priorityService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public DataSeeder(RoleRepository roleRepository,
                      UserRepository userRepository,
                      ResponderProfileRepository responderProfileRepository,
                      ResourceRepository resourceRepository,
                      IncidentRepository incidentRepository,
                      ResourceAssignmentRepository assignmentRepository,
                      PasswordEncoder passwordEncoder,
                      IncidentPriorityService priorityService,
                      NotificationService notificationService,
                      AuditLogService auditLogService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.responderProfileRepository = responderProfileRepository;
        this.resourceRepository = resourceRepository;
        this.incidentRepository = incidentRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.priorityService = priorityService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0 && userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial seeding.");
            return;
        }

        log.info("Initializing ResQCloud realistic seed data...");

        // 1. Seed Roles
        Role citizenRole = roleRepository.save(new Role(RoleType.ROLE_CITIZEN, "Public citizen user with emergency reporting privileges"));
        Role responderRole = roleRepository.save(new Role(RoleType.ROLE_RESPONDER, "First responder with field dispatch and status tracking"));
        Role coordinatorRole = roleRepository.save(new Role(RoleType.ROLE_COORDINATOR, "Emergency operations coordinator with triage and assignment control"));
        Role adminRole = roleRepository.save(new Role(RoleType.ROLE_ADMIN, "System administrator with complete platform access"));

        // 2. Seed Users
        String encodedPass = passwordEncoder.encode("Password123!");

        User adminUser = userRepository.save(new User("admin", "admin@resqcloud.com", encodedPass, "Alexander Vance", "+1-415-555-0100", adminRole));
        User coordinatorUser = userRepository.save(new User("coordinator", "coordinator@resqcloud.com", encodedPass, "Elena Rostova", "+1-415-555-0101", coordinatorRole));

        User responder1 = userRepository.save(new User("medic_sarah", "sarah.medic@resqcloud.com", encodedPass, "Sarah Jenkins", "+1-415-555-0102", responderRole));
        responderProfileRepository.save(new ResponderProfile(responder1, "Advanced Life Support / Paramedic", "MEDIC-402", 37.7749, -122.4194));

        User responder2 = userRepository.save(new User("fire_chief_dave", "dave.fire@resqcloud.com", encodedPass, "David Henderson", "+1-415-555-0103", responderRole));
        responderProfileRepository.save(new ResponderProfile(responder2, "Structural Fire & Hazmat", "FIRE-108", 37.7833, -122.4167));

        User responder3 = userRepository.save(new User("rescue_mike", "mike.rescue@resqcloud.com", encodedPass, "Michael Chen", "+1-415-555-0104", responderRole));
        responderProfileRepository.save(new ResponderProfile(responder3, "Urban Search & Water Rescue", "RESCUE-205", 37.7900, -122.4000));

        User citizen1 = userRepository.save(new User("citizen_john", "john.doe@gmail.com", encodedPass, "John Doe", "+1-415-555-0105", citizenRole));
        User citizen2 = userRepository.save(new User("citizen_emma", "emma.watson@gmail.com", encodedPass, "Emma Watson", "+1-415-555-0106", citizenRole));

        // 3. Seed Resources
        Resource res1 = resourceRepository.save(new Resource("Medic-1 Mobile ICU", ResourceType.AMBULANCE, ResourceStatus.AVAILABLE, 37.7750, -122.4180, "Station 1, 1051 Mission St, San Francisco, CA", 2, "+1-415-555-1101"));
        Resource res2 = resourceRepository.save(new Resource("Ambulance Rapid-3", ResourceType.AMBULANCE, ResourceStatus.ASSIGNED, 37.7600, -122.4200, "Station 7, 2300 Folsom St, San Francisco, CA", 2, "+1-415-555-1102"));
        Resource res3 = resourceRepository.save(new Resource("Engine Company 10", ResourceType.FIRE_ENGINE, ResourceStatus.AVAILABLE, 37.7850, -122.4010, "Station 10, 655 Presidio Ave, San Francisco, CA", 6, "+1-415-555-1103"));
        Resource res4 = resourceRepository.save(new Resource("Heavy Rescue Squad 1", ResourceType.RESCUE_VEHICLE, ResourceStatus.AVAILABLE, 37.7910, -122.3990, "Station 3, 1067 Post St, San Francisco, CA", 4, "+1-415-555-1104"));
        Resource res5 = resourceRepository.save(new Resource("Disaster Medical Assistance Team Alpha", ResourceType.MEDICAL_TEAM, ResourceStatus.AVAILABLE, 37.7790, -122.4150, "Civic Center Logistics Hub, San Francisco, CA", 8, "+1-415-555-1105"));
        Resource res6 = resourceRepository.save(new Resource("Wildland Fire Crew Bravo", ResourceType.FIREFIGHTERS, ResourceStatus.BUSY, 37.7950, -122.4600, "Presidio Fire Depot, San Francisco, CA", 10, "+1-415-555-1106"));
        Resource res7 = resourceRepository.save(new Resource("K-9 Search & Volunteer Unit", ResourceType.VOLUNTEER_TEAM, ResourceStatus.AVAILABLE, 37.7550, -122.4800, "Sunset Emergency Volunteer Post, CA", 5, "+1-415-555-1107"));
        Resource res8 = resourceRepository.save(new Resource("Emergency Response Patrol 4", ResourceType.POLICE_TEAM, ResourceStatus.AVAILABLE, 37.8080, -122.4170, "North Beach Substation, San Francisco, CA", 2, "+1-415-555-1108"));

        // 4. Seed Realistic Incidents across various stages
        // Incident 1: Multi-vehicle accident (Assigned)
        Incident inc1 = new Incident("INC-2026-1041", "Critical Multi-Vehicle Collision on Highway 101",
                "Three-car collision blocking central lanes. Multiple injuries, smoke emanating from engine block.",
                IncidentType.ACCIDENT, IncidentSeverity.CRITICAL, 37.7650, -122.4080, "Hwy 101 Northbound near Cesar Chavez Exit", 5, citizen1);
        inc1.setStatus(IncidentStatus.ASSIGNED);
        inc1.setAssignedResource(res2);
        inc1.setAssignedResponder(responder1);
        Incident savedInc1 = incidentRepository.save(inc1);
        priorityService.calculateAndApplyPriority(savedInc1);

        assignmentRepository.save(new ResourceAssignment(savedInc1.getId(), res2, responder1, AssignmentStatus.ACCEPTED, "Dispatched ALS ambulance unit"));

        // Incident 2: Commercial Fire (Prioritized)
        Incident inc2 = new Incident("INC-2026-1042", "Commercial High-Rise Fire Alarm & Smoke Detected",
                "Heavy black smoke reported from 4th floor commercial kitchen. Evacuation alarms sounding.",
                IncidentType.FIRE, IncidentSeverity.HIGH, 37.7880, -122.4020, "500 Howard St, Financial District, San Francisco", 12, citizen2);
        inc2.setStatus(IncidentStatus.PRIORITIZED);
        Incident savedInc2 = incidentRepository.save(inc2);
        priorityService.calculateAndApplyPriority(savedInc2);

        // Incident 3: Flash Flood (Under Review)
        Incident inc3 = new Incident("INC-2026-1043", "Severe Flash Flooding in Underpass with Trapped Vehicle",
                "Water depth approx 3 feet in roadway underpass. Vehicle stalled with occupants seeking high ground.",
                IncidentType.FLOOD, IncidentSeverity.HIGH, 37.7420, -122.4220, "Bernal Heights Underpass, San Francisco", 3, citizen1);
        inc3.setStatus(IncidentStatus.UNDER_REVIEW);
        Incident savedInc3 = incidentRepository.save(inc3);
        priorityService.calculateAndApplyPriority(savedInc3);

        // Incident 4: Cardiac Emergency (Responder En Route)
        Incident inc4 = new Incident("INC-2026-1044", "Cardiac Arrest in Public Transit Station",
                "Male, approximately 60 years old, collapsed on concourse platform. CPR initiated by bystander.",
                IncidentType.MEDICAL, IncidentSeverity.CRITICAL, 37.7795, -122.4140, "Civic Center BART/Muni Station Concourse", 1, citizen2);
        inc4.setStatus(IncidentStatus.RESPONDER_EN_ROUTE);
        inc4.setAssignedResource(res1);
        inc4.setAssignedResponder(responder1);
        Incident savedInc4 = incidentRepository.save(inc4);
        priorityService.calculateAndApplyPriority(savedInc4);

        // Incident 5: Missing Elderly Person (Reported)
        Incident inc5 = new Incident("INC-2026-1045", "Missing Senior with Memory Impairment in Park",
                "78-year-old female walked away from family picnic. Last seen near playground wearing blue windbreaker.",
                IncidentType.MISSING_PERSON, IncidentSeverity.MEDIUM, 37.7690, -122.4830, "Golden Gate Park, 10th Ave Concourse", 1, citizen1);
        inc5.setStatus(IncidentStatus.REPORTED);
        Incident savedInc5 = incidentRepository.save(inc5);
        priorityService.calculateAndApplyPriority(savedInc5);

        // Incident 6: Resolved Gas Leak (Historical)
        Incident inc6 = new Incident("INC-2026-0982", "Gas Odor and Evacuation in Apartment Complex",
                "Strong natural gas odor reported in basement boiler room of 24-unit apartment building.",
                IncidentType.BUILDING_EMERGENCY, IncidentSeverity.HIGH, 37.7580, -122.4150, "1240 Valencia St, Mission District", 24, citizen2);
        inc6.setStatus(IncidentStatus.RESOLVED);
        inc6.setResolvedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        inc6.setResolutionNotes("Main gas line valve isolated by Engine 10. PG&E crew on scene completed line pressure repair. Building declared safe.");
        Incident savedInc6 = incidentRepository.save(inc6);
        priorityService.calculateAndApplyPriority(savedInc6);

        // Incident 7: Closed Trash Fire (Historical)
        Incident inc7 = new Incident("INC-2026-0950", "Dumpster Fire Adjacent to Loading Dock",
                "Dumpster fire behind grocery store threatening exterior siding.",
                IncidentType.FIRE, IncidentSeverity.LOW, 37.7720, -122.4280, "Market & Octavia St Alleyway", 0, citizen1);
        inc7.setStatus(IncidentStatus.CLOSED);
        inc7.setResolvedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        inc7.setResolutionNotes("Fire extinguished quickly with booster line. No structural extension.");
        Incident savedInc7 = incidentRepository.save(inc7);
        priorityService.calculateAndApplyPriority(savedInc7);

        // 5. Seed initial notifications
        notificationService.notifyUser(citizen1, "Incident Reported", "Your incident #INC-2026-1041 has been prioritized as CRITICAL.", NotificationType.INCIDENT_PRIORITIZED, savedInc1);
        notificationService.notifyUser(responder1, "Dispatched to Incident", "You have been assigned to #INC-2026-1041 on Hwy 101.", NotificationType.RESPONDER_ASSIGNED, savedInc1);
        notificationService.notifyUser(coordinatorUser, "System Alert: High Priority Incident", "Multiple high priority incidents pending coordinator review.", NotificationType.SYSTEM_ALERT, savedInc2);

        // 6. Seed sample audit logs
        auditLogService.record(adminUser.getId(), adminUser.getUsername(), AuditAction.USER_CREATED, "User", adminUser.getId().toString(), null, "ACTIVE", "127.0.0.1", "System bootstrap initialization");
        auditLogService.record(coordinatorUser.getId(), coordinatorUser.getUsername(), AuditAction.INCIDENT_ASSIGNED, "Incident", savedInc1.getId().toString(), "PRIORITIZED", "ASSIGNED", "127.0.0.1", "Assigned Medic-1 and Sarah Jenkins");

        log.info("ResQCloud database initialization completed successfully.");
    }
}
