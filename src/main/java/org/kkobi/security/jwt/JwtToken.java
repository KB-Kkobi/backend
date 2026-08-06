package org.kkobi.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 로그인 성공 시 발급되는 액세스·리프레시 토큰과 각각의 만료 시각
@Getter
@AllArgsConstructor
public class JwtToken {
    private final String tokenType;
    private final String accessToken;
    private final String refreshToken;
    private final long accessTokenExpiresAt;
    private final long refreshTokenExpiresAt;
}
