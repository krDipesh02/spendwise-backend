package com.spendwise.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.spendwise.config.properties.JwtProperties;
import com.spendwise.entity.UserProfile;
import com.spendwise.repository.RefreshTokenRepository;
import com.spendwise.entity.RefreshToken;
import com.spendwise.model.ApplicationRole;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtAuthServiceTest {
    @Test
    void issuedJwtCarriesUserIdentityAndScopes() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKey(pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        properties.setPublicKey(pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        UUID id = UUID.randomUUID();
        UserProfile user = mock(UserProfile.class);
        when(user.getId()).thenReturn(id);
        when(user.getApplicationRole()).thenReturn(ApplicationRole.USER);
        JwtAuthService service = new JwtAuthService(properties, mock(RefreshTokenRepository.class));

        String jwt = service.accessToken(user);
        var authentication = service.authenticate(jwt);

        assertEquals(id, ((com.spendwise.security.principal.AuthenticatedUser) authentication.getPrincipal()).getUserId());
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_expenses:read")));
        assertFalse(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_telegram:claims:read")));
    }

    @Test
    void rejectsTokenWithWrongAudience() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKey(pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        properties.setPublicKey(pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        String token = Jwts.builder().issuer(properties.getIssuer()).audience().add("different-client").and()
                .subject(UUID.randomUUID().toString()).expiration(new java.util.Date(System.currentTimeMillis() + 60_000))
                .signWith(pair.getPrivate(), Jwts.SIG.RS256).compact();
        JwtAuthService service = new JwtAuthService(properties, mock(RefreshTokenRepository.class));
        assertThrows(Exception.class, () -> service.authenticate(token));
    }

    @Test
    void refreshRotatesOpaqueTokenAndStoresOnlyItsHash() throws Exception {
        KeyPair pair = keyPair();
        JwtProperties properties = properties(pair);
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        UUID family = UUID.randomUUID();
        UserProfile user = mock(UserProfile.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getApplicationRole()).thenReturn(ApplicationRole.USER);
        RefreshToken previous = mock(RefreshToken.class);
        when(previous.getRevokedAt()).thenReturn(null);
        when(previous.getExpiresAt()).thenReturn(Instant.now().plusSeconds(60));
        when(previous.getFamilyId()).thenReturn(family);
        when(previous.getUser()).thenReturn(user);
        when(repository.findLockedByTokenHash(any())).thenReturn(java.util.Optional.of(previous));
        JwtAuthService service = new JwtAuthService(properties, repository);

        var result = service.rotate("raw-refresh-token");

        assertNotEquals("raw-refresh-token", result.refreshToken());
        verify(previous).setRevokedAt(any(Instant.class));
        verify(repository).save(argThat(saved -> saved.getTokenHash().length() == 64
                && !saved.getTokenHash().equals(result.refreshToken())
                && saved.getFamilyId().equals(family)));
    }

    @Test
    void replayedRefreshTokenRevokesTheActiveFamily() throws Exception {
        KeyPair pair = keyPair();
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        UUID family = UUID.randomUUID();
        RefreshToken replayed = mock(RefreshToken.class);
        RefreshToken active = mock(RefreshToken.class);
        when(replayed.getRevokedAt()).thenReturn(Instant.now());
        when(replayed.getFamilyId()).thenReturn(family);
        when(repository.findLockedByTokenHash(any())).thenReturn(java.util.Optional.of(replayed));
        when(repository.findActiveFamily(family)).thenReturn(java.util.List.of(active));
        JwtAuthService service = new JwtAuthService(properties(pair), repository);

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.rotate("replayed-token"));
        verify(active).setRevokedAt(any(Instant.class));
    }

    private static String pem(String label, byte[] encoded) {
        return "-----BEGIN " + label + "-----\n" + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(encoded)
                + "\n-----END " + label + "-----";
    }

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static JwtProperties properties(KeyPair pair) {
        JwtProperties properties = new JwtProperties();
        properties.setPrivateKey(pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        properties.setPublicKey(pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        return properties;
    }
}
