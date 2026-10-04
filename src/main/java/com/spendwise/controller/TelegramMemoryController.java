package com.spendwise.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendwise.security.ServiceTokenVerifier;
import com.spendwise.repository.TelegramAccountRepository;
import com.spendwise.service.TelegramAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/telegram/users/{telegramUserId}/memory")
@Slf4j
public class TelegramMemoryController {
    private final TelegramAccountRepository accounts;
    private final TelegramAuthorizationService authorizationService;
    private final ServiceTokenVerifier tokenVerifier;
    private final ObjectMapper objectMapper;

    public TelegramMemoryController(TelegramAccountRepository accounts, TelegramAuthorizationService authorizationService,
                                    ServiceTokenVerifier tokenVerifier, ObjectMapper objectMapper) {
        this.accounts = accounts;
        this.authorizationService = authorizationService;
        this.tokenVerifier = tokenVerifier;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public JsonNode get(@RequestHeader(value = "Authorization", required = false) String authorization,
                        @PathVariable String telegramUserId) {
        tokenVerifier.requireTelegramServiceToken(authorization);
        var account = authorizationService.getActiveAccount(telegramUserId);
        return parse(account.getMemoryJson());
    }

    @PutMapping
    @Transactional
    public JsonNode put(@RequestHeader(value = "Authorization", required = false) String authorization,
                        @PathVariable String telegramUserId, @RequestBody JsonNode memory) {
        tokenVerifier.requireTelegramServiceToken(authorization);
        var account = authorizationService.getActiveAccount(telegramUserId);
        account.setMemoryJson(memory == null || memory.isNull() ? "{}" : memory.toString());
        accounts.save(account);
        return parse(account.getMemoryJson());
    }

    @DeleteMapping
    @Transactional
    public void delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                       @PathVariable String telegramUserId) {
        tokenVerifier.requireTelegramServiceToken(authorization);
        var account = authorizationService.getActiveAccount(telegramUserId);
        account.setMemoryJson(null);
        accounts.save(account);
    }

    private JsonNode parse(String value) {
        if (value == null || value.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored Telegram memory is invalid");
        }
    }
}
