# ResQCloud — System Architecture & Design Documentation

## Intelligent Emergency Response & Resource Coordination Platform

---

## 1. Executive Architecture Summary

ResQCloud is engineered as a **Modular Monolith** applying **Domain-Driven Design (DDD)**, **Layered Architecture**, and **Clean Architecture Principles**. The application decouples business logic from frameworks and external communication transports, enabling straightforward extraction into microservices or cloud-native cloud services (e.g., AWS ECS, RDS, SQS, SNS) in future phases without touching core domain rules.

---

## 2. High-Level System Architecture

```mermaid
graph TD
    Client["Client Browser (SPA) / Mobile"]
    
    subgraph Spring_Boot_Backend["Spring Boot 3.3+ Application Layer"]
        Security["Spring Security + JWT Filter"]
        
        subgraph Controllers["REST Controllers"]
            AuthController["AuthController (/api/auth)"]
            IncidentController["IncidentController (/api/incidents)"]
            ResourceController["ResourceController (/api/resources)"]
            DashboardController["DashboardController (/api/dashboard)"]
            AnalyticsController["AnalyticsController (/api/analytics)"]
            AuditController["AuditLogController (/api/audit-logs)"]
            NotifController["NotificationController (/api/notifications)"]
        end
        
        subgraph Domain_Services["Domain & Application Services"]
            AuthService["AuthService"]
            UserService["UserService"]
            IncidentService["IncidentService"]
            PriorityEngine["IncidentPriorityService<br/>(RuleBasedEngine / Strategy)"]
            StateEngine["IncidentStateTransitionService<br/>(State Machine)"]
            ResourceService["ResourceService"]
            MatchingEngine["ResourceMatchingService<br/>(Heuristic / Multi-factor)"]
            NotifService["NotificationService<br/>(Local / WebSocket Broker)"]
            AuditService["AuditLogService<br/>(Immutable Log Engine)"]
            AnalyticsService["AnalyticsService"]
        end
        
        subgraph Repositories["Spring Data JPA Repositories"]
            UserRepo["UserRepository / RoleRepository"]
            IncidentRepo["IncidentRepository"]
            HistoryRepo["IncidentStatusHistoryRepository"]
            ResourceRepo["ResourceRepository"]
            AssignRepo["ResourceAssignmentRepository"]
            NotifRepo["NotificationRepository"]
            AuditRepo["AuditLogRepository"]
        end
    end
    
    subgraph Data_Storage["Relational Database"]
        DB[(PostgreSQL / H2 Dev)]
    end
    
    Client -->|HTTP REST / WebSocket| Security
    Security --> Controllers
    Controllers --> Domain_Services
    Domain_Services --> Repositories
    Repositories --> DB
```

---

## 3. Incident Lifecycle State Machine

The incident lifecycle follows a strictly enforced deterministic state machine handled by [`IncidentStateTransitionService`](file:///c:/Users/Admin/OneDrive/Documents/placementDrive/AWSbased%20Project/ResQcloud/src/main/java/com/resqcloud/incident/service/IncidentStateTransitionService.java). Invalid transitions are rejected with HTTP 409 Conflict.

```mermaid
stateDiagram-v2
    [*] --> REPORTED: Citizen Reports Emergency
    
    REPORTED --> UNDER_REVIEW: Coordinator Reviews
    REPORTED --> PRIORITIZED: Automated Priority Engine
    REPORTED --> CANCELLED: False Alarm / Duplicate
    
    UNDER_REVIEW --> PRIORITIZED: Priority Calculated
    UNDER_REVIEW --> ASSIGNED: Immediate Dispatch
    UNDER_REVIEW --> CANCELLED: Cancelled
    
    PRIORITIZED --> ASSIGNED: Resource & Responder Dispatched
    PRIORITIZED --> UNDER_REVIEW: Needs Further Assessment
    PRIORITIZED --> CANCELLED: Cancelled
    
    ASSIGNED --> RESPONDER_EN_ROUTE: Unit Mobilized
    ASSIGNED --> IN_PROGRESS: Unit On Scene
    ASSIGNED --> PRIORITIZED: Unit Reassigned / Aborted
    
    RESPONDER_EN_ROUTE --> IN_PROGRESS: Unit Arrived On Scene
    RESPONDER_EN_ROUTE --> ASSIGNED: Dispatched Unit Rerouted
    
    IN_PROGRESS --> RESOLVED: Incident Mitigated / Controlled
    IN_PROGRESS --> ASSIGNED: Escalation / Backup Required
    
    RESOLVED --> CLOSED: Administrative Sign-Off
    RESOLVED --> IN_PROGRESS: Reopened (Relapse / Additional Threat)
    
    CLOSED --> UNDER_REVIEW: Formal Administrative Reopening
    CLOSED --> [*]
    CANCELLED --> [*]
```

---

## 4. End-to-End Request & Incident Flow

```mermaid
sequenceDiagram
    autonumber
    actor Citizen
    actor Coordinator
    actor Responder
    participant API as IncidentController / REST API
    participant Engine as IncidentPriorityService
    participant Matcher as ResourceMatchingService
    participant Notif as NotificationService
    participant State as IncidentStateTransitionService
    participant DB as Relational DB (PostgreSQL)

    Citizen->>API: POST /api/incidents (Emergency Report)
    API->>DB: Save Incident (status = REPORTED)
    API->>Engine: calculateAndApplyPriority()
    Engine-->>API: Priority Score: 79.0 (HIGH)
    API->>Notif: broadcast(/topic/incidents, alert)
    API-->>Citizen: 201 Created (Incident #INC-2026-1041)

    Coordinator->>API: GET /api/resources/matching/{id}
    API->>Matcher: matchResources(incident, limit=5)
    Matcher->>DB: Query available resources within radius
    Matcher-->>Coordinator: Ranked resources (capability, distance, workload)

    Coordinator->>API: POST /api/resources/{id}/assign (Ambulance + Responder)
    API->>DB: Create ResourceAssignment & Update statuses
    API->>State: transitionState(ASSIGNED)
    API->>Notif: notifyResponderAssigned()
    
    Responder->>API: POST /api/incidents/{id}/status (target: IN_PROGRESS)
    API->>State: validateTransition(ASSIGNED -> IN_PROGRESS)
    API->>DB: Record Status History & Audit Log
    
    Responder->>API: POST /api/incidents/{id}/status (target: RESOLVED)
    API->>State: validateTransition(IN_PROGRESS -> RESOLVED)
    API->>DB: Update resolvedAt & resolution notes
    API->>Notif: notifyIncidentResolved()
```

---

## 5. Intelligent Multi-Factor Priority Calculation Engine

The Priority Calculation Engine implements the Strategy Pattern via [`PriorityCalculationStrategy`](file:///c:/Users/Admin/OneDrive/Documents/placementDrive/AWSbased%20Project/ResQcloud/src/main/java/com/resqcloud/incident/service/priority/PriorityCalculationStrategy.java), allowing easy replacement with an ML-based regression/classification model in the future:

$$\text{Total Score} = \min\left(100, S_{\text{sev}} + S_{\text{people}} + S_{\text{type}} + S_{\text{time}} + S_{\text{resource}}\right)$$

* **Severity Score ($S_{\text{sev}}$)**: Max 35 points (Low: 8.75, Medium: 19.25, High: 29.75, Critical: 35.0)
* **Affected People Score ($S_{\text{people}}$)**: Max 25 points based on victim headcount brackets
* **Incident Type Score ($S_{\text{type}}$)**: Max 20 points (Fire: 20, Collapse: 19, Flood: 18, Medical: 17, Accident: 15, Missing: 13, Other: 8)
* **Time Sensitivity Score ($S_{\text{time}}$)**: Max 10 points (increases linearly as minutes pass without response)
* **Resource Availability Pressure ($S_{\text{resource}}$)**: Max 10 points (scales inverse to available response fleet)

### Threshold Normalization:
* **0 – 30**: `LOW`
* **31 – 60**: `MEDIUM`
* **61 – 80**: `HIGH`
* **81 – 100**: `CRITICAL`

---

## 6. Intelligent Resource Matching Algorithm

The Resource Matching Engine scores all eligible resources using Haversine great-circle distance, required capability fit, and existing operational workload:

$$\text{Match Score} = (\text{Capability Fit} \times 0.40) + (\text{Proximity Score} \times 0.35) + (\text{Workload Score} \times 0.25)$$

* **Capability Fit**: $100\%$ for Primary unit match (e.g. Fire Engine for Fire), $70\%$ for Secondary unit match (e.g. Rescue Vehicle for Fire), $30\%$ for Generic unit.
* **Proximity Score**: $\max(0, 100 - (\text{DistanceInKm} \times 2.0))$
* **Workload Score**: $\max(0, 100 - (\text{ActiveAssignments} \times 50.0))$

---

## 7. Database Entity-Relationship (ER) Model

```mermaid
erDiagram
    ROLES ||--o{ USERS : assigns
    USERS ||--o{ RESPONDER_PROFILES : has
    USERS ||--o{ INCIDENTS : reports
    USERS ||--o{ INCIDENTS : assigned_responder
    RESOURCES ||--o{ INCIDENTS : assigned_resource
    INCIDENTS ||--o{ INCIDENT_STATUS_HISTORY : tracks
    USERS ||--o{ INCIDENT_STATUS_HISTORY : changed_by
    INCIDENTS ||--o{ RESOURCE_ASSIGNMENTS : receives
    RESOURCES ||--o{ RESOURCE_ASSIGNMENTS : assigned_in
    USERS ||--o{ RESOURCE_ASSIGNMENTS : assigned_responder
    USERS ||--o{ NOTIFICATIONS : receives
    INCIDENTS ||--o{ NOTIFICATIONS : references
    USERS ||--o{ AUDIT_LOGS : performs

    ROLES {
        bigint id PK
        varchar name UK
        varchar description
    }

    USERS {
        uuid id PK
        varchar username UK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar phone_number
        bigint role_id FK
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }

    RESPONDER_PROFILES {
        uuid id PK
        uuid user_id FK,UK
        varchar specialization
        varchar badge_number UK
        varchar availability_status
        double current_latitude
        double current_longitude
    }

    RESOURCES {
        uuid id PK
        varchar name
        varchar resource_type
        varchar status
        double latitude
        double longitude
        varchar address
        int capacity
        varchar contact_number
    }

    INCIDENTS {
        uuid id PK
        varchar incident_number UK
        varchar title
        text description
        varchar incident_type
        varchar severity
        varchar status
        double priority_score
        varchar priority_level
        double latitude
        double longitude
        varchar address
        int affected_people
        uuid reporter_id FK
        uuid assigned_responder_id FK
        uuid assigned_resource_id FK
        text resolution_notes
        timestamp resolved_at
        timestamp created_at
    }

    INCIDENT_STATUS_HISTORY {
        uuid id PK
        uuid incident_id FK
        varchar from_status
        varchar to_status
        uuid changed_by_user_id FK
        text comments
        timestamp changed_at
    }

    RESOURCE_ASSIGNMENTS {
        uuid id PK
        uuid incident_id FK
        uuid resource_id FK
        uuid responder_id FK
        varchar status
        timestamp assigned_at
        timestamp released_at
        text notes
    }

    NOTIFICATIONS {
        uuid id PK
        uuid recipient_id FK
        varchar title
        text message
        varchar type
        uuid related_incident_id FK
        boolean is_read
        timestamp created_at
    }

    AUDIT_LOGS {
        uuid id PK
        uuid user_id FK
        varchar username
        varchar action
        varchar entity_name
        varchar entity_id
        text previous_state
        text new_state
        varchar ip_address
        text metadata
        timestamp created_at
    }
```

---

## 8. Future Cloud & AWS Migration Architecture

*Note: For this initial phase, the application runs entirely standalone with no external cloud dependencies. The architecture is purposefully designed so that cloud infrastructure can be plugged in seamlessly in subsequent phases.*

```mermaid
graph TD
    User["Citizen / Responder / Operator"]
    
    subgraph AWS_Edge_Security["Edge & Security Layer (Future)"]
        WAF["AWS WAF"]
        CloudFront["Amazon CloudFront (CDN)"]
        Route53["Amazon Route 53 (DNS)"]
    end
    
    subgraph AWS_Compute_VPC["VPC Compute & Application Tier"]
        ALB["Application Load Balancer (ALB)"]
        ECS_Cluster["Amazon ECS (Fargate) / EKS"]
        AppTask1["Spring Boot Task 1 (Container)"]
        AppTask2["Spring Boot Task 2 (Container)"]
    end
    
    subgraph AWS_Async_Messaging["Event & Asynchronous Services"]
        EventBridge["Amazon EventBridge"]
        SQS["Amazon SQS (Priority Queue)"]
        SNS["Amazon SNS (Citizen SMS / Push)"]
    end
    
    subgraph AWS_Data_Persistence["Persistence & Storage Tier"]
        RDS["Amazon RDS (PostgreSQL Multi-AZ)"]
        S3["Amazon S3 (Incident Evidence / Attachments)"]
        ElastiCache["Amazon ElastiCache (Redis Session/Cache)"]
    end
    
    subgraph AWS_Observability["Monitoring & Observability"]
        CloudWatch["Amazon CloudWatch Logs & Metrics"]
        XRay["AWS X-Ray (Distributed Tracing)"]
    end

    User --> Route53
    Route53 --> CloudFront
    CloudFront --> WAF
    WAF --> ALB
    ALB --> AppTask1 & AppTask2
    AppTask1 & AppTask2 --> RDS
    AppTask1 & AppTask2 --> S3
    AppTask1 & AppTask2 --> ElastiCache
    AppTask1 & AppTask2 --> EventBridge
    EventBridge --> SQS
    EventBridge --> SNS
    AppTask1 & AppTask2 -.-> CloudWatch
    AppTask1 & AppTask2 -.-> XRay
```

### Abstraction Mappings:
1. `NotificationService` interface:
   - Current: `LocalNotificationService` (STOMP WebSocket + DB)
   - Future: `AwsNotificationService` (Amazon SNS + Pinpoint)
2. Asynchronous Event Processing:
   - Current: Spring Application Events / `@Async`
   - Future: Amazon EventBridge + Amazon SQS
3. Relational Persistence:
   - Current: PostgreSQL / in-memory PostgreSQL mode
   - Future: Amazon RDS for PostgreSQL with Read Replicas
4. File Storage:
   - Current: Local multipart storage
   - Future: Amazon S3 with Pre-signed URLs
