package com.spendwise.service;

import com.spendwise.config.SeedDataConfig;
import com.spendwise.config.TelegramProperties;
import com.spendwise.dto.entity.TelegramAccount;
import com.spendwise.dto.entity.TelegramInvite;
import com.spendwise.dto.entity.UserProfile;
import com.spendwise.dto.repository.TelegramAccountRepository;
import com.spendwise.dto.repository.TelegramInviteRepository;
import com.spendwise.dto.repository.UserProfileRepository;
import com.spendwise.model.TelegramAccountStatus;
import com.spendwise.model.TelegramInviteStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TelegramAuthorizationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final TelegramAccountRepository accounts;
    private final TelegramInviteRepository invites;
    private final UserProfileRepository users;
    private final TelegramProperties properties;
    private final SeedDataConfig seedData;

    public TelegramAuthorizationService(TelegramAccountRepository accounts, TelegramInviteRepository invites,
                                        UserProfileRepository users, TelegramProperties properties,
                                        SeedDataConfig seedData) {
        this.accounts = accounts;
        this.invites = invites;
        this.users = users;
        this.properties = properties;
        this.seedData = seedData;
    }

    @Transactional
    public InviteCreated createInvite() {
        String botUsername = properties.getBotUsername() == null ? "" : properties.getBotUsername().trim().replaceFirst("^@", "");
        if (botUsername.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram bot username is not configured");
        int minutes = properties.getInviteExpirationMinutes();
        if (minutes < 1) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram invite expiration must be positive");
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        TelegramInvite invite = new TelegramInvite();
        invite.setTokenHash(sha256(token));
        invite.setStatus(TelegramInviteStatus.ACTIVE);
        invite.setExpiresAt(Instant.now().plus(minutes, ChronoUnit.MINUTES));
        invite.setCreatedBy("configured-admin");
        invite = invites.save(invite);
        log.info("Telegram invite created inviteId={} expiresAt={}", invite.getId(), invite.getExpiresAt());
        return new InviteCreated(invite.getId(), "https://t.me/" + botUsername + "?start=" + token, invite.getExpiresAt(), minutes);
    }

    @Transactional
    public ClaimResult claim(String telegramUserId, String rawToken, String username, String firstName, String lastName) {
        if (telegramUserId == null || telegramUserId.isBlank() || rawToken == null || rawToken.isBlank()) {
            return new ClaimResult(ClaimStatus.INVALID_INVITE, null);
        }
        String normalizedId = telegramUserId.trim();
        TelegramInvite invite = invites.findByTokenHashForUpdate(sha256(rawToken.trim())).orElse(null);
        if (invite == null) return new ClaimResult(ClaimStatus.INVALID_INVITE, null);

        if (invite.getStatus() == TelegramInviteStatus.USED) {
            if (normalizedId.equals(invite.getUsedByTelegramUserId())) {
                TelegramAccount account = accounts.findByTelegramUserIdForUpdate(normalizedId).orElse(null);
                if (account != null && account.getStatus() == TelegramAccountStatus.ACTIVE) {
                    return new ClaimResult(ClaimStatus.ALREADY_ACTIVE, account.getUser().getId());
                }
                if (account != null && account.getStatus() == TelegramAccountStatus.BLOCKED) {
                    return new ClaimResult(ClaimStatus.ACCOUNT_BLOCKED, account.getUser().getId());
                }
            }
            return new ClaimResult(ClaimStatus.INVITE_ALREADY_USED, null);
        }
        if (invite.getStatus() == TelegramInviteStatus.REVOKED) return new ClaimResult(ClaimStatus.INVITE_REVOKED, null);
        if (invite.getStatus() == TelegramInviteStatus.EXPIRED) return new ClaimResult(ClaimStatus.INVITE_EXPIRED, null);
        if (invite.getStatus() == TelegramInviteStatus.CLAIMED) {
            return normalizedId.equals(invite.getUsedByTelegramUserId())
                    ? new ClaimResult(ClaimStatus.PENDING_APPROVAL, null)
                    : new ClaimResult(ClaimStatus.INVITE_ALREADY_USED, null);
        }
        if (!invite.getExpiresAt().isAfter(Instant.now())) {
            invite.setStatus(TelegramInviteStatus.EXPIRED);
            return new ClaimResult(ClaimStatus.INVITE_EXPIRED, null);
        }

        TelegramAccount linkedAccount = accounts.findByTelegramUserIdForUpdate(normalizedId).orElse(null);
        UserProfile legacyOwner = users.findByTelegramId(normalizedId).orElse(null);
        if (linkedAccount != null || legacyOwner != null) {
            UserProfile owner = linkedAccount == null ? legacyOwner : linkedAccount.getUser();
            if (linkedAccount != null && linkedAccount.getStatus() == TelegramAccountStatus.BLOCKED) {
                return new ClaimResult(ClaimStatus.ACCOUNT_BLOCKED, owner.getId());
            }
            return new ClaimResult(ClaimStatus.TELEGRAM_ALREADY_LINKED, owner.getId());
        }

        invite.setStatus(TelegramInviteStatus.CLAIMED);
        invite.setUsedByTelegramUserId(normalizedId); // Binds the one-time invite to its first claimant.
        invite.setClaimedAt(Instant.now());
        invite.setTelegramUsername(clean(username));
        invite.setTelegramFirstName(clean(firstName));
        invite.setTelegramLastName(clean(lastName));
        log.info("Telegram invite claimed; awaiting admin approval inviteId={} telegramUserId={}", invite.getId(), normalizedId);
        return new ClaimResult(ClaimStatus.PENDING_APPROVAL, null);
    }

    @Transactional(readOnly = true)
    public List<PendingClaim> listPendingClaims() {
        return invites.findByStatusOrderByClaimedAtAsc(TelegramInviteStatus.CLAIMED).stream()
                .map(invite -> new PendingClaim(invite.getId(), invite.getUsedByTelegramUserId(),
                        invite.getTelegramUsername(), invite.getTelegramFirstName(), invite.getTelegramLastName(),
                        invite.getClaimedAt(), invite.getExpiresAt()))
                .toList();
    }

    @Transactional
    public ApprovalResult approveClaim(UUID inviteId) {
        TelegramInvite invite = invites.findByIdForUpdate(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Telegram claim not found"));
        if (invite.getStatus() != TelegramInviteStatus.CLAIMED || invite.getUsedByTelegramUserId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telegram claim is not pending approval");
        }
        String telegramUserId = invite.getUsedByTelegramUserId();
        TelegramAccount existing = accounts.findByTelegramUserIdForUpdate(telegramUserId).orElse(null);
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telegram account is already linked");
        }

        UserProfile user = users.findByTelegramId(telegramUserId).orElseGet(() -> createTelegramUser(invite, telegramUserId));
        TelegramAccount account = new TelegramAccount();
        account.setTelegramUserId(telegramUserId);
        account.setUser(user);
        account.setStatus(TelegramAccountStatus.ACTIVE);
        account.setLinkedAt(Instant.now());
        if (user.getTelegramMemoryJson() != null) account.setMemoryJson(user.getTelegramMemoryJson());
        accounts.save(account);

        invite.setUser(user);
        invite.setStatus(TelegramInviteStatus.USED);
        invite.setUsedAt(Instant.now());
        log.info("Telegram claim approved inviteId={} telegramUserId={} userId={}", inviteId, telegramUserId, user.getId());
        return new ApprovalResult(inviteId, telegramUserId, user.getId());
    }

    private UserProfile createTelegramUser(TelegramInvite invite, String telegramUserId) {
        UserProfile user = new UserProfile();
        user.setTelegramId(telegramUserId);
        String displayName = String.join(" ", List.of(
                invite.getTelegramFirstName() == null ? "" : invite.getTelegramFirstName(),
                invite.getTelegramLastName() == null ? "" : invite.getTelegramLastName()).stream().filter(part -> !part.isBlank()).toList());
        user.setDisplayName(displayName.isBlank() ? "Telegram user " + telegramUserId : displayName);
        user.setBaseCurrency("INR");
        user.setTimezone("Asia/Kolkata");
        user.setMonthlyLimit(BigDecimal.ZERO);
        UserProfile saved = users.save(user);
        seedData.seedDefaultCategories(saved);
        return saved;
    }

    @Transactional
    public void rejectClaim(UUID inviteId) {
        TelegramInvite invite = invites.findByIdForUpdate(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Telegram claim not found"));
        if (invite.getStatus() != TelegramInviteStatus.CLAIMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telegram claim is not pending approval");
        }
        invite.setStatus(TelegramInviteStatus.REVOKED);
        log.info("Telegram claim rejected inviteId={} telegramUserId={}", inviteId, invite.getUsedByTelegramUserId());
    }

    @Transactional(readOnly = true)
    public AuthorizationResult getAuthorization(String telegramUserId) {
        var account = accounts.findByTelegramUserId(telegramUserId);
        if (account.isPresent()) {
            return new AuthorizationResult(account.get().getStatus().name(), account.get().getUser().getId());
        }
        if (invites.findFirstByUsedByTelegramUserIdAndStatus(telegramUserId, TelegramInviteStatus.CLAIMED).isPresent()) {
            return new AuthorizationResult("PENDING_APPROVAL", null);
        }
        return new AuthorizationResult("NOT_FOUND", null);
    }

    @Transactional(readOnly = true)
    public TelegramAccount getActiveAccount(String telegramUserId) {
        TelegramAccount account = accounts.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Telegram account is not authorized"));
        if (account.getStatus() != TelegramAccountStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Telegram account is blocked");
        return account;
    }

    @Transactional
    public void revokeInvite(UUID inviteId) {
        TelegramInvite invite = invites.findByIdForUpdate(inviteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (invite.getStatus() != TelegramInviteStatus.ACTIVE && invite.getStatus() != TelegramInviteStatus.CLAIMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active or pending invitations can be revoked");
        }
        invite.setStatus(TelegramInviteStatus.REVOKED);
    }

    @Transactional
    public void setAccountStatus(String telegramUserId, TelegramAccountStatus status) {
        TelegramAccount account = accounts.findByTelegramUserIdForUpdate(telegramUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Telegram account not found"));
        account.setStatus(status);
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().substring(0, Math.min(value.trim().length(), 128));
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public record InviteCreated(UUID inviteId, String inviteUrl, Instant expiresAt, int expiresInMinutes) {}
    public record ClaimResult(ClaimStatus status, UUID userId) {}
    public record AuthorizationResult(String status, UUID userId) {}
    public record PendingClaim(UUID inviteId, String telegramUserId, String username, String firstName,
                               String lastName, Instant claimedAt, Instant inviteExpiresAt) {}
    public record ApprovalResult(UUID inviteId, String telegramUserId, UUID userId) {}
    public enum ClaimStatus { PENDING_APPROVAL, ALREADY_ACTIVE, INVALID_INVITE, INVITE_EXPIRED, INVITE_REVOKED, INVITE_ALREADY_USED, TELEGRAM_ALREADY_LINKED, ACCOUNT_BLOCKED }
}
