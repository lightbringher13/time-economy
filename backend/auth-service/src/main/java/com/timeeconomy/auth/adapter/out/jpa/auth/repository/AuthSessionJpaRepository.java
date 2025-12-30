package com.timeeconomy.auth.adapter.out.jpa.auth.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.timeeconomy.auth.adapter.out.jpa.auth.entity.AuthSessionEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionEntity, Long> {

    Optional<AuthSessionEntity> findByTokenHash(String tokenHash);

    List<AuthSessionEntity> findByUserIdAndRevokedFalse(Long userId);

    @Query("""
        SELECT s FROM AuthSessionEntity s
         WHERE s.familyId = :familyId
           AND s.revoked = false
           AND s.expiresAt > :now
         ORDER BY s.expiresAt DESC
    """)
    Optional<AuthSessionEntity> findLatestActiveByFamily(
            @Param("familyId") String familyId,
            @Param("now") Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSessionEntity s WHERE s.tokenHash = :tokenHash")
    AuthSessionEntity findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
        UPDATE AuthSessionEntity s
           SET s.revoked = true,
               s.revokedAt = :now
         WHERE s.familyId = :familyId
           AND s.revoked = false
    """)
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);
}