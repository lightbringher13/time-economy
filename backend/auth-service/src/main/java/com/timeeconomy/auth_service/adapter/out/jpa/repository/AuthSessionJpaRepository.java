package com.timeeconomy.auth_service.adapter.out.jpa.repository;

import com.timeeconomy.auth_service.adapter.out.jpa.entity.AuthSessionEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionEntity, Long> {

    Optional<AuthSessionEntity> findByTokenHash(String tokenHash);

    List<AuthSessionEntity> findByUserIdAndRevokedFalse(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSessionEntity s WHERE s.tokenHash = :tokenHash")
    AuthSessionEntity findByTokenHashForUpdate(String tokenHash);

    @Modifying
    @Query("""
        UPDATE AuthSessionEntity s
           SET s.revoked = true,
               s.revokedAt = :now
         WHERE s.familyId = :familyId
           AND s.revoked = false
    """)
    int revokeFamily(String familyId, LocalDateTime now);
}