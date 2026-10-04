package com.spendwise.entity;

import com.spendwise.model.BaseEntity;
import com.spendwise.model.TelegramInviteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "telegram_invites",
        indexes = @Index(name = "idx_telegram_invites_status_expiry", columnList = "status, expires_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_telegram_invites_token_hash", columnNames = "token_hash"))
public class TelegramInvite extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserProfile user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TelegramInviteStatus status = TelegramInviteStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by_telegram_user_id", length = 64)
    private String usedByTelegramUserId;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "telegram_username", length = 64)
    private String telegramUsername;

    @Column(name = "telegram_first_name", length = 128)
    private String telegramFirstName;

    @Column(name = "telegram_last_name", length = 128)
    private String telegramLastName;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public TelegramInviteStatus getStatus() { return status; }
    public void setStatus(TelegramInviteStatus status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public String getUsedByTelegramUserId() { return usedByTelegramUserId; }
    public void setUsedByTelegramUserId(String usedByTelegramUserId) { this.usedByTelegramUserId = usedByTelegramUserId; }
    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }
    public String getTelegramUsername() { return telegramUsername; }
    public void setTelegramUsername(String telegramUsername) { this.telegramUsername = telegramUsername; }
    public String getTelegramFirstName() { return telegramFirstName; }
    public void setTelegramFirstName(String telegramFirstName) { this.telegramFirstName = telegramFirstName; }
    public String getTelegramLastName() { return telegramLastName; }
    public void setTelegramLastName(String telegramLastName) { this.telegramLastName = telegramLastName; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
