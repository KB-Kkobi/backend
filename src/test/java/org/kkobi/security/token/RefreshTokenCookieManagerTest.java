package org.kkobi.security.token;

import org.junit.jupiter.api.Test;
import org.kkobi.exception.InvalidRefreshTokenException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCookieManagerTest {

    private final RefreshTokenCookieManager cookieManager =
            new RefreshTokenCookieManager(true, "Strict");

    @Test
    void readsRefreshTokenFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(RefreshTokenCookieManager.COOKIE_NAME, "refresh-value"));

        assertEquals("refresh-value", cookieManager.read(request));
    }

    @Test
    void rejectsRequestWithoutRefreshTokenCookie() {
        assertThrows(
                InvalidRefreshTokenException.class,
                () -> cookieManager.read(new MockHttpServletRequest())
        );
    }

    @Test
    void writesAndClearsSecureHttpOnlyCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        cookieManager.write(response, "refresh-value", System.currentTimeMillis() + 60_000);

        String issuedCookie = response.getHeader("Set-Cookie");
        assertTrue(issuedCookie.contains("refresh_token=refresh-value"));
        assertTrue(issuedCookie.contains("Path=/api/auth"));
        assertTrue(issuedCookie.contains("Secure"));
        assertTrue(issuedCookie.contains("HttpOnly"));
        assertTrue(issuedCookie.contains("SameSite=Strict"));

        MockHttpServletResponse clearResponse = new MockHttpServletResponse();
        cookieManager.clear(clearResponse);
        assertTrue(clearResponse.getHeader("Set-Cookie").contains("Max-Age=0"));
    }
}
