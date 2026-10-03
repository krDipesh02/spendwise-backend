package com.spendwise.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BrowserOriginVerifier {
    private final String allowedOrigin;
    public BrowserOriginVerifier(@Value("${spendwise.frontend-origin:http://localhost:5173}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin.replaceAll("/$", "");
    }
    public void requireSameOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || !allowedOrigin.equals(origin.replaceAll("/$", "")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request origin is not allowed");
    }
}
