package com.spendwise.dto.repository;

import com.spendwise.dto.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByTelegramId(String telegramId);

    Optional<UserProfile> findByGoogleSubject(String googleSubject);

    Optional<UserProfile> findByUsername(String username);
}
