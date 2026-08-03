package org.kkobi.security.util;

import lombok.RequiredArgsConstructor;
import org.kkobi.security.jwt.JwtProvider;
import org.springframework.stereotype.Component;

// 기존 호출부와의 호환을 유지하는 JWT 처리 어댑터
@Component
@RequiredArgsConstructor
public class JwtProcessor {
    private final JwtProvider jwtProvider;

    // 인증된 사용자 식별자로 액세스 토큰을 생성
    public String generateToken(String subject) {
        return jwtProvider.createAccessToken(subject);
    }

    // 서명된 JWT에서 사용자 식별자를 추출
    public String getUsername(String token) {
        return jwtProvider.getSubject(token);
    }

    // JWT 서명과 만료 시간이 유효한지 검증
    public boolean validateToken(String token) {
        return jwtProvider.validateAccessToken(token);
    }
}
