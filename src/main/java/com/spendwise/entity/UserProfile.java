package com.spendwise.entity;

import com.spendwise.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.spendwise.model.ApplicationRole;

import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class UserProfile extends BaseEntity {

    @Column(unique = true)
    private String telegramId;

    @Column(name = "telegram_memory_json", columnDefinition = "TEXT")
    private String telegramMemoryJson;

    @Column(unique = true)
    private String googleSubject;

    @Column(unique = true)
    private String username;

    @Column(length = 200)
    private String passwordHash;

    @Column
    private String email;

    @Column
    private boolean emailVerified;

    @Column(length = 1000)
    private String pictureUrl;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String baseCurrency;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_role", length = 20)
    private ApplicationRole applicationRole = ApplicationRole.USER;

    public ApplicationRole getApplicationRole() {
        return applicationRole == null ? ApplicationRole.USER : applicationRole;
    }

    public void setApplicationRole(ApplicationRole applicationRole) {
        this.applicationRole = applicationRole;
    }

    public String getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }

    public String getTelegramMemoryJson() {
        return telegramMemoryJson;
    }

    public void setTelegramMemoryJson(String telegramMemoryJson) {
        this.telegramMemoryJson = telegramMemoryJson;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public boolean isGoogleLinked() {
        return googleSubject != null && !googleSubject.isBlank();
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }
}
