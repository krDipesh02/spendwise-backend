package com.spendwise.dto.repository;

import com.spendwise.dto.entity.TelegramCredentialSetupToken;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelegramCredentialSetupTokenRepository extends JpaRepository<TelegramCredentialSetupToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TelegramCredentialSetupToken t join fetch t.user where t.tokenHash = :hash")
    Optional<TelegramCredentialSetupToken> findByHashForUpdate(@Param("hash") String hash);
}
