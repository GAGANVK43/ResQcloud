package com.resqcloud.resource.repository;

import com.resqcloud.resource.entity.Resource;
import com.resqcloud.resource.entity.ResourceStatus;
import com.resqcloud.resource.entity.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByStatus(ResourceStatus status);

    List<Resource> findByResourceTypeAndStatus(ResourceType resourceType, ResourceStatus status);

    @Query("SELECT r FROM Resource r WHERE " +
           "(:type IS NULL OR r.resourceType = :type) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.address) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Resource> filterResources(@Param("type") ResourceType type,
                                   @Param("status") ResourceStatus status,
                                   @Param("search") String search,
                                   Pageable pageable);

    long countByStatus(ResourceStatus status);
}
