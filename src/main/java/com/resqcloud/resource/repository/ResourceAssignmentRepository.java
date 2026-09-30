package com.resqcloud.resource.repository;

import com.resqcloud.resource.entity.AssignmentStatus;
import com.resqcloud.resource.entity.ResourceAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceAssignmentRepository extends JpaRepository<ResourceAssignment, UUID> {

    List<ResourceAssignment> findByIncidentId(UUID incidentId);

    List<ResourceAssignment> findByResourceId(UUID resourceId);

    List<ResourceAssignment> findByResponderIdAndStatus(UUID responderId, AssignmentStatus status);

    Optional<ResourceAssignment> findTopByResourceIdAndStatusInOrderByAssignedAtDesc(UUID resourceId, List<AssignmentStatus> statuses);

    @Query("SELECT COUNT(ra) FROM ResourceAssignment ra WHERE ra.resource.id = :resourceId AND ra.status IN ('PENDING', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE')")
    long countActiveAssignmentsForResource(@Param("resourceId") UUID resourceId);

    @Query("SELECT ra FROM ResourceAssignment ra WHERE ra.responder.id = :responderId AND ra.status IN ('PENDING', 'ACCEPTED', 'EN_ROUTE', 'ON_SCENE')")
    List<ResourceAssignment> findActiveAssignmentsForResponder(@Param("responderId") UUID responderId);
}
