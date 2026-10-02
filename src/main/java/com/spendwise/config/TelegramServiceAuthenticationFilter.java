package com.spendwise.config;

import com.spendwise.dto.repository.TelegramAccountRepository;
import com.spendwise.model.TelegramAccountStatus;
import com.spendwise.utils.AuthenticatedUser;
import com.spendwise.utils.AuthenticationType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class TelegramServiceAuthenticationFilter extends OncePerRequestFilter {
    private final TelegramAccountRepository accounts;
    private final ServiceTokenVerifier tokenVerifier;

    public TelegramServiceAuthenticationFilter(TelegramAccountRepository accounts, ServiceTokenVerifier tokenVerifier) {
        this.accounts = accounts;
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String telegramUserId = request.getHeader("X-Telegram-User-Id");
        if (telegramUserId == null || telegramUserId.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (!tokenVerifier.isValidAuthorization(request.getHeader("Authorization"))) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid service credential");
            return;
        }
        var account = accounts.findByTelegramUserId(telegramUserId.trim()).orElse(null);
        if (account == null) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "Telegram account is not authorized");
            return;
        }
        if (account.getStatus() != TelegramAccountStatus.ACTIVE) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "Telegram account is blocked");
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(account.getUser().getId(), AuthenticationType.TELEGRAM_SERVICE),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TELEGRAM_SERVICE")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
