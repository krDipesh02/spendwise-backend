package com.spendwise.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/** Logs request metadata for every HTTP request without recording headers or payloads. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        long startedAt = System.nanoTime();
        try {
            log.info("HTTP request started: method={} path={}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("HTTP request failed: method={} path={} exceptionType={}",
                    request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName(), ex);
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("HTTP request completed: method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            MDC.remove("requestId");
        }
    }
}
