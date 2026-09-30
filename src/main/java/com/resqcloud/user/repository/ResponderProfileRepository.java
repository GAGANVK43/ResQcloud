package com.resqcloud.user.repository;

import com.resqcloud.user.entity.ResponderProfile;
import com.resqcloud.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResponderProfileRepository extends JpaRepository<ResponderProfile, UUID> {
    Optional<ResponderProfile> findByUser(User user);
    Optional<ResponderProfile> findByUserId(UUID userId);
    
    @Query("SELECT rp FROM ResponderProfile rp WHERE rp.availabilityStatus = 'AVAILABLE' AND rp.user.active = true")
    List<ResponderProfile> findAllAvailable();

    @Query("SELECT rp FROM ResponderProfile rp WHERE rp.availabilityStatus = 'AVAILABLE' AND rp.specialization = :specialization AND rp.user.active = true")
    List<ResponderProfile> findAvailableBySpecialization(@Param("specialization") String specialization);
}
