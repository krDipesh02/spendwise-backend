package com.spendwise.service;

import com.spendwise.config.properties.JwtProperties;
import com.spendwise.entity.RefreshToken;
import com.spendwise.entity.UserProfile;
import com.spendwise.repository.RefreshTokenRepository;
import com.spendwise.model.ApplicationRole;
import com.spendwise.security.principal.AuthenticationType;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class JwtAuthService {
    public static final String REFRESH_COOKIE = "SPENDWISE_REFRESH";
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokens;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtAuthService(JwtProperties properties, RefreshTokenRepository refreshTokens) {
        this.properties = properties;
        this.refreshTokens = refreshTokens;
        this.privateKey = parsePrivateKey(properties.getPrivateKey());
        this.publicKey = parsePublicKey(properties.getPublicKey());
    }

    public TokenResult createLogin(UserProfile user) {
        return createSession(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw unauthorized();
        RefreshToken old = refreshTokens.findLockedByTokenHash(hash(rawToken)).orElseThrow(JwtAuthService::unauthorized);
        Instant now = Instant.now();
        if (old.getRevokedAt() != null) {
            refreshTokens.findActiveFamily(old.getFamilyId()).forEach(t -> t.setRevokedAt(now));
            throw unauthorized();
        }
        if (!old.getExpiresAt().isAfter(now)) {
            old.setRevokedAt(now);
            throw unauthorized();
        }
        old.setRevokedAt(now);
        return createSession(old.getUser(), old.getFamilyId());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findLockedByTokenHash(hash(rawToken)).ifPresent(token -> {
            Instant now = Instant.now();
            token.setRevokedAt(now);
            refreshTokens.findActiveFamily(token.getFamilyId()).forEach(active -> active.setRevokedAt(now));
        });
    }

    public ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(properties.isRefreshCookieSecure())
                .sameSite(properties.getRefreshCookieSameSite()).path(properties.getRefreshCookiePath())
                .maxAge(maxAgeSeconds).build();
    }

    public String accessToken(UserProfile user) {
        if (privateKey == null) throw new IllegalStateException("JWT signing key is not configured");
        Instant now = Instant.now();
        List<String> scopes = scopes(user.getApplicationRole());
        return Jwts.builder().issuer(properties.getIssuer()).audience().add(properties.getAudience()).and()
                .subject(user.getId().toString()).id(UUID.randomUUID().toString()).issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusSeconds(properties.getAccessTokenMinutes() * 60)))
                .claim("roles", List.of(user.getApplicationRole().name())).claim("scope", String.join(" ", scopes))
                .signWith(privateKey, Jwts.SIG.RS256).compact();
    }

    public Authentication authenticate(String token) {
        if (publicKey == null) throw new IllegalStateException("JWT verification key is not configured");
        var claims = Jwts.parser().verifyWith(publicKey).requireIssuer(properties.getIssuer()).build()
                .parseSignedClaims(token).getPayload();
        if (!claims.getAudience().contains(properties.getAudience())) throw unauthorized();
        UUID userId = UUID.fromString(claims.getSubject());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Object rawScope = claims.get("scope");
        if (rawScope instanceof String scope) {
            for (String item : scope.split("\\s+")) if (!item.isBlank()) authorities.add(new SimpleGrantedAuthority("SCOPE_" + item));
        }
        Object rawRoles = claims.get("roles");
        if (rawRoles instanceof List<?> roles) roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        authorities.add(new SimpleGrantedAuthority("ROLE_JWT_USER"));
        return new UsernamePasswordAuthenticationToken(new com.spendwise.security.principal.AuthenticatedUser(userId, AuthenticationType.JWT), null, authorities);
    }

    public List<String> scopes(ApplicationRole role) {
        List<String> result = new ArrayList<>(List.of("profile:read", "profile:write", "expenses:read", "expenses:write", "budgets:read", "budgets:write", "categories:read", "categories:write", "analytics:read", "api-keys:manage"));
        if (role == ApplicationRole.ADMIN) result.addAll(List.of("telegram:invites:manage", "telegram:claims:read", "telegram:claims:manage", "telegram:users:manage"));
        return result;
    }

    public long getAccessTokenExpiresInSeconds() { return properties.getAccessTokenMinutes() * 60; }
    public long getRefreshTokenTtlSeconds() { return properties.getRefreshTokenDays() * 86400; }

    private TokenResult createSession(UserProfile user, UUID familyId) {
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes());
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plusSeconds(getRefreshTokenTtlSeconds()));
        refreshTokens.save(token);
        return new TokenResult(accessToken(user), raw, getAccessTokenExpiresInSeconds(), user, scopes(user.getApplicationRole()));
    }

    private byte[] randomBytes() { byte[] bytes = new byte[48]; new java.security.SecureRandom().nextBytes(bytes); return bytes; }
    private String hash(String raw) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired"); }
    private static PrivateKey parsePrivateKey(String pem) {
        if (pem == null || pem.isBlank()) return null;
        try { return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decodePem(pem, "PRIVATE KEY"))); }
        catch (Exception e) { throw new IllegalStateException("Invalid SPENDWISE_JWT_PRIVATE_KEY (expected PKCS8 PEM)", e); }
    }
    private static PublicKey parsePublicKey(String pem) {
        if (pem == null || pem.isBlank()) return null;
        try { return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodePem(pem, "PUBLIC KEY"))); }
        catch (Exception e) { throw new IllegalStateException("Invalid SPENDWISE_JWT_PUBLIC_KEY (expected X509 PEM)", e); }
    }
    private static byte[] decodePem(String pem, String label) {
        return Base64.getDecoder().decode(pem.replace("\\n", "\n").replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "").replaceAll("\\s", ""));
    }

    public record TokenResult(String accessToken, String refreshToken, long expiresIn, UserProfile user, List<String> scopes) { }
}
