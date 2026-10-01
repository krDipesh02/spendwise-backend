package com.spendwise.dto.repository;

import com.spendwise.dto.entity.UserApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserApiKeyRepository extends JpaRepository<UserApiKey, UUID> {

    Optional<UserApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

    List<UserApiKey> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<UserApiKey> findByIdAndUserId(UUID id, UUID userId);
}
