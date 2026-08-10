package org.kkobi.security.token;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.kkobi.exception.InvalidRefreshTokenException;
import org.kkobi.security.jwt.JwtProvider;
import org.kkobi.security.jwt.JwtToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    // 로그인 성공 시 토큰 쌍을 발급하고 Refresh Token 해시를 Redis에 저장
    public JwtToken issue(String subject) {
        JwtToken token = jwtProvider.issueToken(subject);
        save(token.getRefreshToken(), token.getRefreshTokenExpiresAt());
        return token;
    }

    // 기존 Refresh Token을 한 번만 소비하고 새 토큰 쌍으로 교체
    public JwtToken reissue(String refreshToken) {
        RefreshTokenInfo tokenInfo = parse(refreshToken);
        if (!refreshTokenStore.consume(tokenInfo.tokenId(), hash(refreshToken))) {
            throw new InvalidRefreshTokenException("유효하지 않거나 이미 사용된 Refresh Token입니다.");
        }

        return issue(tokenInfo.subject());
    }

    // 로그아웃은 같은 요청을 반복해도 안전하도록 이미 폐기된 토큰도 성공으로 처리
    public void revoke(String refreshToken) {
        RefreshTokenInfo tokenInfo = parse(refreshToken);
        refreshTokenStore.consume(tokenInfo.tokenId(), hash(refreshToken));
    }

    private void save(String refreshToken, long expiresAt) {
        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            throw new InvalidRefreshTokenException("만료된 Refresh Token입니다.");
        }

        refreshTokenStore.save(
                jwtProvider.getTokenId(refreshToken),
                hash(refreshToken),
                Duration.ofMillis(remainingMillis)
        );
    }

    private RefreshTokenInfo parse(String refreshToken) {
        try {
            if (!jwtProvider.validateRefreshToken(refreshToken)) {
                throw new InvalidRefreshTokenException("Refresh Token 형식이 아닙니다.");
            }
            String tokenId = jwtProvider.getTokenId(refreshToken);
            String subject = jwtProvider.getSubject(refreshToken);
            if (tokenId == null || tokenId.isBlank() || subject == null || subject.isBlank()) {
                throw new InvalidRefreshTokenException("Refresh Token 필수 정보가 없습니다.");
            }
            return new RefreshTokenInfo(tokenId, subject);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidRefreshTokenException("유효하지 않거나 만료된 Refresh Token입니다.", ex);
        }
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private record RefreshTokenInfo(String tokenId, String subject) {
    }
}
