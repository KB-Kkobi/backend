package org.kkobi.users.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kkobi.exception.InvalidRefreshTokenException;
import org.kkobi.security.jwt.JwtProvider;
import org.kkobi.security.jwt.JwtToken;
import org.kkobi.security.token.RefreshTokenCookieManager;
import org.kkobi.security.token.RefreshTokenService;
import org.kkobi.security.token.RefreshTokenStore;
import org.kkobi.users.dto.response.TokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTokenCookieTest {
    private static final String SUBJECT = "user@example.com";
    private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long";

    private RefreshTokenService refreshTokenService;
    private RefreshTokenCookieManager cookieManager;
    private UserController controller;

    @BeforeEach
    void setUp() {
        JwtProvider jwtProvider = new JwtProvider(
                SECRET,
                "kkobi-test",
                1_800_000L,
                1_209_600_000L
        );
        refreshTokenService = new RefreshTokenService(jwtProvider, new InMemoryRefreshTokenStore());
        cookieManager = new RefreshTokenCookieManager(true, "Strict");
        controller = new UserController(null, refreshTokenService, cookieManager);
    }

    @Test
    void refreshRotatesCookieWithoutExposingRefreshTokenInBody() {
        JwtToken original = refreshTokenService.issue(SUBJECT);
        MockHttpServletRequest request = requestWithRefreshToken(original.getRefreshToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<TokenResponse> result = controller.refresh(request, response);

        assertNotNull(result.getBody());
        assertNotNull(result.getBody().getAccessToken());
        String rotatedCookie = response.getHeader("Set-Cookie");
        assertNotNull(rotatedCookie);
        assertFalse(rotatedCookie.contains(original.getRefreshToken()));
        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.reissue(original.getRefreshToken())
        );
    }

    @Test
    void logoutRevokesRefreshTokenAndExpiresCookie() {
        JwtToken token = refreshTokenService.issue(SUBJECT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(requestWithRefreshToken(token.getRefreshToken()), response);

        String clearedCookie = response.getHeader("Set-Cookie");
        assertNotNull(clearedCookie);
        assertTrue(clearedCookie.contains("Max-Age=0"));
        assertThrows(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.reissue(token.getRefreshToken())
        );
    }

    private MockHttpServletRequest requestWithRefreshToken(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RefreshTokenCookieManager.COOKIE_NAME, refreshToken));
        return request;
    }

    private static class InMemoryRefreshTokenStore implements RefreshTokenStore {
        private final Map<String, String> tokens = new HashMap<>();

        @Override
        public void save(String tokenId, String tokenHash, Duration timeToLive) {
            tokens.put(tokenId, tokenHash);
        }

        @Override
        public boolean consume(String tokenId, String tokenHash) {
            return tokens.remove(tokenId, tokenHash);
        }
    }
}
