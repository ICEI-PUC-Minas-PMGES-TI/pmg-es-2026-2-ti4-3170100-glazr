package com.glazr.invitation.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeInvitationRepository extends JpaRepository<EmployeeInvitationEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from EmployeeInvitationEntity invitation where invitation.tokenHash = :tokenHash")
    Optional<EmployeeInvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation from EmployeeInvitationEntity invitation
            where invitation.userId = :userId
              and invitation.consumedAt is null and invitation.revokedAt is null
            """)
    Optional<EmployeeInvitationEntity> findUsableByUserIdForUpdate(@Param("userId") UUID userId);

    Optional<EmployeeInvitationEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
