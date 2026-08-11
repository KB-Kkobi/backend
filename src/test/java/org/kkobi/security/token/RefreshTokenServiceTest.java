package org.kkobi.security.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kkobi.exception.InvalidRefreshTokenException;
import org.kkobi.security.jwt.JwtProvider;
import org.kkobi.security.jwt.JwtToken;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenServiceTest {
    private static final String SUBJECT = "user@example.com";
    private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long";

    private JwtProvider jwtProvider;
    private InMemoryRefreshTokenStore refreshTokenStore;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, "kkobi-test", 1_800_000L, 1_209_600_000L);
        refreshTokenStore = new InMemoryRefreshTokenStore();
        refreshTokenService = new RefreshTokenService(jwtProvider, refreshTokenStore);
    }

    @Test
    void storesRefreshTokenHashWithTtlWhenIssued() {
        JwtToken token = refreshTokenService.issue(SUBJECT);

        String tokenId = jwtProvider.getTokenId(token.getRefreshToken());
        assertTrue(refreshTokenStore.contains(tokenId));
        assertFalse(refreshTokenStore.storedValue(tokenId).contains(token.getRefreshToken()));
        assertTrue(refreshTokenStore.timeToLive(tokenId).toMillis() > 0);
    }

    @Test
    void rotatesRefreshTokenAndRejectsReuse() {
        JwtToken original = refreshTokenService.issue(SUBJECT);

        JwtToken rotated = refreshTokenService.reissue(original.getRefreshToken());

        assertEquals(SUBJECT, jwtProvider.getSubject(rotated.getAccessToken()));
        assertNotEquals(original.getRefreshToken(), rotated.getRefreshToken());
        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.reissue(original.getRefreshToken()));
        assertTrue(refreshTokenStore.contains(jwtProvider.getTokenId(rotated.getRefreshToken())));
    }

    @Test
    void revokesRefreshTokenIdempotently() {
        JwtToken token = refreshTokenService.issue(SUBJECT);

        refreshTokenService.revoke(token.getRefreshToken());
        refreshTokenService.revoke(token.getRefreshToken());

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.reissue(token.getRefreshToken()));
    }

    @Test
    void rejectsAccessTokenAndTamperedToken() {
        JwtToken token = refreshTokenService.issue(SUBJECT);

        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.reissue(token.getAccessToken()));
        assertThrows(InvalidRefreshTokenException.class,
                () -> refreshTokenService.reissue(token.getRefreshToken() + "tampered"));
    }

    private static class InMemoryRefreshTokenStore implements RefreshTokenStore {
        private final Map<String, Entry> tokens = new HashMap<>();

        @Override
        public void save(String tokenId, String tokenHash, Duration timeToLive) {
            tokens.put(tokenId, new Entry(tokenHash, timeToLive));
        }

        @Override
        public boolean consume(String tokenId, String tokenHash) {
            Entry entry = tokens.get(tokenId);
            if (entry == null || !entry.tokenHash().equals(tokenHash)) {
                return false;
            }
            tokens.remove(tokenId);
            return true;
        }

        boolean contains(String tokenId) {
            return tokens.containsKey(tokenId);
        }

        String storedValue(String tokenId) {
            return tokens.get(tokenId).tokenHash();
        }

        Duration timeToLive(String tokenId) {
            return tokens.get(tokenId).timeToLive();
        }

        private record Entry(String tokenHash, Duration timeToLive) {
        }
    }
}
