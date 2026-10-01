package com.spendwise.dto.service;

import com.spendwise.dto.entity.PasswordResetToken;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.repository.PasswordResetTokenRepository;
import com.spendwise.dto.repository.UserProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@Slf4j
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserProfileRepository userProfileRepository,
                                PasswordEncoder passwordEncoder,
                                AuditService auditService) {
        this.tokenRepository = tokenRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public PasswordResetToken requestReset(String username) {
        log.info("Issuing password reset token for username={}", username);
        UserProfile user = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found for username"));
        byte[] buffer = new byte[32];
        SECURE_RANDOM.nextBytes(buffer);
        String token = "rst_" + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        PasswordResetToken saved = tokenRepository.save(resetToken);

        log.info("Issued password reset token for userId={} expiresAt={}", user.getId(), saved.getExpiresAt());
        auditService.log(user, "REQUEST_PASSWORD_RESET", "USER", user.getId().toString(), token.substring(0, 12));
        return saved;
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        log.info("Confirming password reset");
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedAtIsNull(token)
                .orElseThrow(() -> new EntityNotFoundException("Reset token not found"));
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            log.error("Password reset token expired for userId={}", resetToken.getUser().getId());
            throw new IllegalArgumentException("Reset token expired");
        }
        UserProfile user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsedAt(Instant.now());
        log.info("Completed password reset for userId={}", user.getId());
        auditService.log(user, "CONFIRM_PASSWORD_RESET", "USER", user.getId().toString(), "PASSWORD_RESET");
    }
}
