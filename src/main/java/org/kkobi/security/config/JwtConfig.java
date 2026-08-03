package org.kkobi.security.config;

import org.kkobi.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 환경변수 기반 JWT 설정을 애플리케이션 Bean으로 등록
@Configuration
public class JwtConfig {

    @Bean
    public JwtProvider jwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs
    ) {
        return new JwtProvider(secret, issuer, accessTokenValidityMs, refreshTokenValidityMs);
    }
}
