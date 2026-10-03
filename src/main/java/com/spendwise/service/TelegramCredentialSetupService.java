package com.spendwise.service;

import com.spendwise.dto.entity.TelegramCredentialSetupToken;
import com.spendwise.dto.repository.TelegramAccountRepository;
import com.spendwise.dto.repository.TelegramCredentialSetupTokenRepository;
import com.spendwise.dto.repository.UserProfileRepository;
import com.spendwise.model.TelegramAccountStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TelegramCredentialSetupService {
    private final TelegramAccountRepository accounts;
    private final UserProfileRepository users;
    private final TelegramCredentialSetupTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public TelegramCredentialSetupService(TelegramAccountRepository accounts, UserProfileRepository users,
            TelegramCredentialSetupTokenRepository tokens, PasswordEncoder passwordEncoder,
            @Value("${spendwise.frontend-base-url:http://localhost:5173}") String frontendUrl) {
        this.accounts = accounts; this.users = users; this.tokens = tokens;
        this.passwordEncoder = passwordEncoder; this.frontendUrl = frontendUrl;
    }

    @Transactional
    public String issueSetupUrl(String telegramUserId) {
        var account = accounts.findByTelegramUserId(telegramUserId)
                .filter(a -> a.getStatus() == TelegramAccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Telegram account is not active"));
        if (account.getUser().getPasswordHash() != null || account.getUser().getUsername() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Web credentials are already configured");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TelegramCredentialSetupToken token = new TelegramCredentialSetupToken();
        token.setUser(account.getUser()); token.setTokenHash(sha256(raw)); token.setExpiresAt(Instant.now().plusSeconds(1800));
        tokens.save(token);
        return frontendUrl.replaceAll("/$", "") + "/setup-password?token=" + raw;
    }

    @Transactional
    public void configureCredentials(String rawToken, String username, String password) {
        if (rawToken == null || username == null || password == null || username.trim().length() < 3 || password.length() < 8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username or password does not meet requirements");
        var token = tokens.findByHashForUpdate(sha256(rawToken)).orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "Setup link is invalid or expired"));
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.GONE, "Setup link is invalid or expired");
        if (token.getUser().getUsername() != null || token.getUser().getPasswordHash() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Credentials are already configured");
        if (users.findByUsername(username.trim()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        token.getUser().setUsername(username.trim());
        token.getUser().setPasswordHash(passwordEncoder.encode(password));
        token.setUsedAt(Instant.now());
    }

    private String sha256(String raw) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
