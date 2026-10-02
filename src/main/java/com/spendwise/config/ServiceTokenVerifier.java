package com.spendwise.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ServiceTokenVerifier {
    private final AuthProperties properties;

    public ServiceTokenVerifier(AuthProperties properties) {
        this.properties = properties;
    }

    public boolean isValidAuthorization(String authorization) {
        String expected = properties.getAutomationServiceToken();
        if (expected == null || expected.isBlank() || sameSecret(expected, properties.getTelegramServiceToken())) return false;
        return isBearerTokenValid(authorization, expected);
    }

    public void requireServiceToken(String authorization) {
        String expected = properties.getAutomationServiceToken();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service authentication is not configured");
        }
        if (!isValidAuthorization(authorization)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid service credential");
        }
    }

    public void requireTelegramServiceToken(String authorization) {
        String expected = properties.getTelegramServiceToken();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram service authentication is not configured");
        }
        if (sameSecret(expected, properties.getAutomationServiceToken())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram and MCP service credentials must be distinct");
        }
        if (!isBearerTokenValid(authorization, expected)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Telegram service credential");
        }
    }

    public void requireAdminToken(String authorization) {
        String expected = properties.getTelegramAdminToken();
        if (expected == null || expected.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram admin credential is not configured");
        }
        String[] serviceTokens = {properties.getAutomationServiceToken(), properties.getTelegramServiceToken()};
        for (String serviceToken : serviceTokens) {
            if (serviceToken != null && !serviceToken.isBlank()
                    && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), serviceToken.getBytes(StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Admin and service credentials must be distinct");
            }
        }
        String supplied = bearerValue(authorization);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credential");
        }
    }

    private boolean sameSecret(String left, String right) {
        return left != null && right != null && !left.isBlank() && !right.isBlank()
                && MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBearerTokenValid(String authorization, String expected) {
        String supplied = bearerValue(authorization);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }

    private String bearerValue(String authorization) {
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7).trim() : "";
    }
}
