package org.kkobi.security.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kkobi.security.jwt.JwtProvider;
import org.kkobi.security.token.RefreshTokenService;
import org.kkobi.security.token.RefreshTokenStore;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUsernamePasswordAuthenticationFilterTest {
    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "correct-password";
    private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JwtProvider jwtProvider;
    private JwtUsernamePasswordAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, "kkobi-test", 1_800_000L, 1_209_600_000L);
        AuthenticationManager authenticationManager = authentication -> {
            if (!EMAIL.equals(authentication.getName())
                    || !PASSWORD.equals(authentication.getCredentials())) {
                throw new BadCredentialsException("인증 정보가 올바르지 않습니다.");
            }
            return new UsernamePasswordAuthenticationToken(
                    authentication.getName(), null, Collections.emptyList());
        };
        RefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        RefreshTokenService refreshTokenService = new RefreshTokenService(jwtProvider, refreshTokenStore);
        filter = new JwtUsernamePasswordAuthenticationFilter(authenticationManager, refreshTokenService);
    }

    @Test
    void returnsAccessAndRefreshTokensForValidCredentials() throws Exception {
        MockHttpServletResponse response = performLogin(
                "{\"email\":\"user@example.com\",\"password\":\"correct-password\"}");

        assertEquals(200, response.getStatus());
        String contentType = response.getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.startsWith("application/json"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals("Bearer", body.get("tokenType").asText());
        assertEquals(EMAIL, jwtProvider.getSubject(body.get("accessToken").asText()));
        assertEquals(EMAIL, jwtProvider.getSubject(body.get("refreshToken").asText()));
        assertTrue(jwtProvider.validateAccessToken(body.get("accessToken").asText()));
        assertTrue(jwtProvider.validateRefreshToken(body.get("refreshToken").asText()));
        assertTrue(body.get("refreshTokenExpiresAt").asLong()
                > body.get("accessTokenExpiresAt").asLong());
    }

    @Test
    void returnsUnauthorizedForWrongPassword() throws Exception {
        MockHttpServletResponse response = performLogin(
                "{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}");

        assertEquals(401, response.getStatus());
        assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.",
                objectMapper.readTree(response.getContentAsString()).get("message").asText());
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        assertBadRequest("{not-json}");
    }

    @Test
    void returnsBadRequestForInvalidEmail() throws Exception {
        assertBadRequest("{\"email\":\"not-an-email\",\"password\":\"password\"}");
    }

    @Test
    void returnsBadRequestForBlankPassword() throws Exception {
        assertBadRequest("{\"email\":\"user@example.com\",\"password\":\"  \"}");
    }

    private void assertBadRequest(String json) throws Exception {
        MockHttpServletResponse response = performLogin(json);

        assertEquals(400, response.getStatus());
        assertEquals("로그인 요청 형식이 올바르지 않습니다.",
                objectMapper.readTree(response.getContentAsString()).get("message").asText());
    }

    private MockHttpServletResponse performLogin(String json) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.setContentType("application/json");
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            throw new AssertionError("로그인 요청은 인증 필터에서 처리되어야 합니다.");
        };

        filter.doFilter(request, response, chain);
        assertFalse(response.getContentAsString().isEmpty());
        return response;
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
