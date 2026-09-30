package com.resqcloud.incident.repository;

import com.resqcloud.incident.entity.Incident;
import com.resqcloud.incident.entity.IncidentSeverity;
import com.resqcloud.incident.entity.IncidentStatus;
import com.resqcloud.incident.entity.IncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByIncidentNumber(String incidentNumber);

    Page<Incident> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);

    Page<Incident> findByAssignedResponderIdOrderByCreatedAtDesc(UUID responderId, Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE " +
           "(:type IS NULL OR i.incidentType = :type) AND " +
           "(:severity IS NULL OR i.severity = :severity) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:reporterId IS NULL OR i.reporter.id = :reporterId) AND " +
           "(:responderId IS NULL OR i.assignedResponder.id = :responderId) AND " +
           "(:search IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(i.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(i.address) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(i.incidentNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Incident> searchAndFilterIncidents(
            @Param("type") IncidentType type,
            @Param("severity") IncidentSeverity severity,
            @Param("status") IncidentStatus status,
            @Param("reporterId") UUID reporterId,
            @Param("responderId") UUID responderId,
            @Param("search") String search,
            Pageable pageable);

    long countByStatus(IncidentStatus status);

    long countBySeverity(IncidentSeverity severity);

    long countByIncidentType(IncidentType type);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status NOT IN ('RESOLVED', 'CLOSED', 'CANCELLED')")
    long countActiveIncidents();

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.severity = 'CRITICAL' AND i.status NOT IN ('RESOLVED', 'CLOSED', 'CANCELLED')")
    long countActiveCriticalIncidents();

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.priorityScore >= 61.0 AND i.status NOT IN ('RESOLVED', 'CLOSED', 'CANCELLED')")
    long countActiveHighPriorityIncidents();

    @Query("SELECT i FROM Incident i WHERE i.createdAt BETWEEN :startDate AND :endDate ORDER BY i.createdAt ASC")
    List<Incident> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
