package org.kkobi.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long";

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProvider jwtProvider = new JwtProvider(
                SECRET, "kkobi-test", 1_800_000L, 1_209_600_000L
        );
        UserDetailsService userDetailsService = username -> {
            throw new AssertionError("Public auth endpoints must not load a user");
        };
        filter = new JwtAuthenticationFilter(jwtProvider, userDetailsService);
    }

    @Test
    void ignoresExpiredOrInvalidAccessHeaderOnRefreshEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.setServletPath("/api/auth/refresh");
        request.addHeader("Authorization", "Bearer expired-or-invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(continued.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsInvalidAccessTokenOnProtectedEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setServletPath("/api/users/me");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean continued = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> continued.set(true);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertFalse(continued.get());
    }
}
