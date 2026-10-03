package com.spendwise.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTokenVerifierTest {
    @Test
    void acceptsOnlyConfiguredBearerServiceToken() {
        AuthProperties properties = new AuthProperties();
        properties.setAutomationServiceToken("service-secret");
        ServiceTokenVerifier verifier = new ServiceTokenVerifier(properties);
        assertTrue(verifier.isValidAuthorization("Bearer service-secret"));
        assertFalse(verifier.isValidAuthorization("Bearer wrong"));
        assertFalse(verifier.isValidAuthorization("service-secret"));
    }

    @Test
    void rejectsMissingServiceCredential() {
        ServiceTokenVerifier verifier = new ServiceTokenVerifier(new AuthProperties());
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> verifier.requireServiceToken("Bearer anything"));
        assertEquals(503, exception.getStatusCode().value());
    }
    @Test
    void rejectsSharedBotAndMcpServiceCredential() {
        AuthProperties properties = new AuthProperties();
        properties.setAutomationServiceToken("same-secret");
        properties.setTelegramServiceToken("same-secret");
        ServiceTokenVerifier verifier = new ServiceTokenVerifier(properties);

        assertFalse(verifier.isValidAuthorization("Bearer same-secret"));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> verifier.requireTelegramServiceToken("Bearer same-secret"));
        assertEquals(503, exception.getStatusCode().value());
    }

}
