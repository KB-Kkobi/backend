package org.kkobi.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

// 액세스·리프레시 JWT를 발급하고 서명, 발급자, 만료 시간, 토큰 유형을 검증
public class JwtProvider {
    public static final String TOKEN_TYPE = "Bearer";

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final int MINIMUM_SECRET_BYTES = 32;

    private final Key signingKey;
    private final String issuer;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtProvider(
            String secret,
            String issuer,
            long accessTokenValidityMs,
            long refreshTokenValidityMs
    ) {
        byte[] secretBytes = requireSecret(secret).getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes long");
        }
        if (issuer == null || issuer.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT issuer must not be blank");
        }
        if (accessTokenValidityMs <= 0 || refreshTokenValidityMs <= 0) {
            throw new IllegalArgumentException("JWT validity must be greater than zero");
        }

        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.issuer = issuer;
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    // 동일한 사용자 식별자로 액세스 토큰과 리프레시 토큰을 함께 발급
    public JwtToken issueToken(String subject) {
        requireSubject(subject);

        long issuedAt = System.currentTimeMillis();
        long accessTokenExpiresAt = issuedAt + accessTokenValidityMs;
        long refreshTokenExpiresAt = issuedAt + refreshTokenValidityMs;

        return new JwtToken(
                TOKEN_TYPE,
                createToken(subject, ACCESS_TOKEN_TYPE, issuedAt, accessTokenExpiresAt),
                createToken(subject, REFRESH_TOKEN_TYPE, issuedAt, refreshTokenExpiresAt),
                accessTokenExpiresAt,
                refreshTokenExpiresAt
        );
    }

    // 기존 단일 액세스 토큰 발급 코드와의 호환을 위한 메서드
    public String createAccessToken(String subject) {
        requireSubject(subject);
        long issuedAt = System.currentTimeMillis();
        return createToken(subject, ACCESS_TOKEN_TYPE, issuedAt, issuedAt + accessTokenValidityMs);
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public boolean validateRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    private String createToken(String subject, String tokenType, long issuedAt, long expiresAt) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setSubject(subject)
                .setIssuedAt(new Date(issuedAt))
                .setExpiration(new Date(expiresAt))
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT must not be blank");
        }

        return Jwts.parserBuilder()
                .requireIssuer(issuer)
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String requireSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT_SECRET must not be blank");
        }
        return secret;
    }

    private void requireSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT subject must not be blank");
        }
    }
}
