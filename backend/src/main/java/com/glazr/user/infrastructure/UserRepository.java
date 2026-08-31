package com.glazr.user.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select distinct u from UserEntity u
            where (:query is null or lower(u.email) like lower(concat('%', :query, '%'))
                or lower(u.displayName) like lower(concat('%', :query, '%')))
              and (:active is null or u.active = :active)
            """)
    Page<UserEntity> search(@Param("query") String query, @Param("active") Boolean active, Pageable pageable);

    @Query("""
            select count(distinct u.id) from UserEntity u join u.accessProfiles p
            where u.active = true and p.id = :profileId
            """)
    long countActiveByProfileId(@Param("profileId") UUID profileId);

    long countByAccessProfilesId(UUID profileId);
}
