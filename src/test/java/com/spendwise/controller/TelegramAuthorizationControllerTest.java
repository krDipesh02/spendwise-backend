package com.spendwise.controller;

import com.spendwise.config.AuthProperties;
import com.spendwise.config.ServiceTokenVerifier;
import com.spendwise.service.TelegramAuthorizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TelegramAuthorizationControllerTest {
    @Test
    void lookupRejectsUnauthenticatedServiceCaller() {
        var controller = new TelegramAuthorizationController(
                mock(TelegramAuthorizationService.class),
                new ServiceTokenVerifier(new AuthProperties()));
        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> controller.lookup(null, "12345"));
        assertEquals(503, exception.getStatusCode().value());
    }
}
