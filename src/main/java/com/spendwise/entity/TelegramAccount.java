package com.spendwise.entity;

import com.spendwise.model.BaseEntity;
import com.spendwise.model.TelegramAccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "telegram_accounts", indexes = @Index(name = "idx_telegram_accounts_user_id", columnList = "user_id"))
public class TelegramAccount extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @Column(name = "telegram_user_id", nullable = false, unique = true, length = 64)
    private String telegramUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TelegramAccountStatus status = TelegramAccountStatus.ACTIVE;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "memory_json", columnDefinition = "TEXT")
    private String memoryJson;

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }
    public String getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(String telegramUserId) { this.telegramUserId = telegramUserId; }
    public TelegramAccountStatus getStatus() { return status; }
    public void setStatus(TelegramAccountStatus status) { this.status = status; }
    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) { this.linkedAt = linkedAt; }
    public String getMemoryJson() { return memoryJson; }
    public void setMemoryJson(String memoryJson) { this.memoryJson = memoryJson; }
}
