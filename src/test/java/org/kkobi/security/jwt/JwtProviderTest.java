package org.kkobi.security.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProviderTest {
    private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long";

    private final JwtProvider jwtProvider = new JwtProvider(
            SECRET,
            "kkobi-test",
            30 * 60 * 1000L,
            14 * 24 * 60 * 60 * 1000L
    );

    @Test
    void issuesAccessAndRefreshTokens() {
        JwtToken token = jwtProvider.issueToken("user@example.com");

        assertEquals("Bearer", token.getTokenType());
        assertNotEquals(token.getAccessToken(), token.getRefreshToken());
        assertEquals("user@example.com", jwtProvider.getSubject(token.getAccessToken()));
        assertEquals("user@example.com", jwtProvider.getSubject(token.getRefreshToken()));
        assertFalse(jwtProvider.getTokenId(token.getRefreshToken()).isBlank());
        assertTrue(jwtProvider.validateAccessToken(token.getAccessToken()));
        assertTrue(jwtProvider.validateRefreshToken(token.getRefreshToken()));
        assertTrue(token.getRefreshTokenExpiresAt() > token.getAccessTokenExpiresAt());
    }

    @Test
    void rejectsRefreshTokenAsAccessToken() {
        JwtToken token = jwtProvider.issueToken("user@example.com");

        assertFalse(jwtProvider.validateAccessToken(token.getRefreshToken()));
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtProvider.createAccessToken("user@example.com");

        assertThrows(JwtException.class, () -> jwtProvider.validateAccessToken(token + "tampered"));
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProvider("too-short", "kkobi-test", 1000L, 2000L));
    }
}
