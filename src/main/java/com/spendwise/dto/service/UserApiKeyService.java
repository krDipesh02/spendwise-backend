package com.spendwise.dto.service;

import com.spendwise.dto.entity.UserApiKey;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.repository.UserApiKeyRepository;
import com.spendwise.dto.response.ApiKeyCreatedDto;
import com.spendwise.utils.ApiKeyHasher;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserApiKeyRepository userApiKeyRepository;
    private final AuditService auditService;

    public UserApiKeyService(UserApiKeyRepository userApiKeyRepository, AuditService auditService) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ApiKeyCreatedDto generate(UserProfile user, String name) {
        log.info("Generating API key for userId={} name={}", user.getId(), name);
        byte[] buffer = new byte[32];
        SECURE_RANDOM.nextBytes(buffer);
        String token = "spw_" + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
        String hash = ApiKeyHasher.sha256(token);
        String prefix = token.substring(0, Math.min(token.length(), 16));

        UserApiKey apiKey = new UserApiKey();
        apiKey.setUser(user);
        apiKey.setName(name);
        apiKey.setKeyPrefix(prefix);
        apiKey.setKeyHash(hash);
        UserApiKey saved = userApiKeyRepository.save(apiKey);
        log.info("Generated apiKeyId={} for userId={} prefix={}", saved.getId(), user.getId(), prefix);
        auditService.log(user, "CREATE_API_KEY", "API_KEY", saved.getId().toString(), prefix);
        return new ApiKeyCreatedDto(saved.getId().toString(), saved.getName(), saved.getKeyPrefix(), token);
    }

    @Transactional(readOnly = true)
    public List<UserApiKey> listActive(UserProfile user) {
        log.debug("Listing active API keys for userId={}", user.getId());
        return userApiKeyRepository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void revoke(UserProfile user, UUID apiKeyId) {
        log.info("Revoking apiKeyId={} for userId={}", apiKeyId, user.getId());
        UserApiKey apiKey = userApiKeyRepository.findByIdAndUserId(apiKeyId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("API key not found"));
        apiKey.setRevokedAt(Instant.now());
        log.info("Revoked apiKeyId={} for userId={}", apiKey.getId(), user.getId());
        auditService.log(user, "REVOKE_API_KEY", "API_KEY", apiKey.getId().toString(), apiKey.getKeyPrefix());
    }

    @Transactional
    public UserProfile authenticate(String token) {
        String hash = ApiKeyHasher.sha256(token);
        log.debug("Authenticating API key by hash");
        UserApiKey apiKey = userApiKeyRepository.findByKeyHashAndRevokedAtIsNull(hash)
                .orElseThrow(() -> new EntityNotFoundException("API key not found"));
        apiKey.setLastUsedAt(Instant.now());
        log.debug("Authenticated API key for userId={} apiKeyId={}", apiKey.getUser().getId(), apiKey.getId());
        return apiKey.getUser();
    }
}
