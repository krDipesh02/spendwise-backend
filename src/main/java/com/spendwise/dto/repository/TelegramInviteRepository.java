package com.spendwise.dto.repository;

import com.spendwise.dto.entity.TelegramInvite;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TelegramInviteRepository extends JpaRepository<TelegramInvite, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invite from TelegramInvite invite where invite.tokenHash = :tokenHash")
    Optional<TelegramInvite> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invite from TelegramInvite invite where invite.id = :id")
    Optional<TelegramInvite> findByIdForUpdate(@Param("id") UUID id);

    List<TelegramInvite> findByStatusOrderByClaimedAtAsc(com.spendwise.model.TelegramInviteStatus status);

    Optional<TelegramInvite> findFirstByUsedByTelegramUserIdAndStatus(String telegramUserId,
                                                                        com.spendwise.model.TelegramInviteStatus status);
}
