package com.spendwise.dto.repository;

import com.spendwise.dto.entity.TelegramAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, UUID> {
    Optional<TelegramAccount> findByTelegramUserId(String telegramUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from TelegramAccount account where account.telegramUserId = :telegramUserId")
    Optional<TelegramAccount> findByTelegramUserIdForUpdate(@Param("telegramUserId") String telegramUserId);
}
