package org.kkobi.security.filter;

import org.kkobi.security.jwt.JwtToken;
import org.kkobi.security.token.RefreshTokenCookieManager;
import org.kkobi.security.token.RefreshTokenService;
import org.kkobi.security.util.JsonResponse;
import org.kkobi.users.dto.request.LoginRequest;
import org.kkobi.users.dto.response.MessageResponse;
import org.kkobi.users.dto.response.TokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    // 아이디와 비밀번호 로그인에 사용할 URL과 JSON 응답 핸들러를 설정
    public JwtUsernamePasswordAuthenticationFilter(
            AuthenticationManager authenticationManager,
            RefreshTokenService refreshTokenService,
            RefreshTokenCookieManager refreshTokenCookieManager
    ) {
        super(authenticationManager);
        setFilterProcessesUrl("/api/auth/login");
        setAuthenticationSuccessHandler((request, response, authentication) -> {
            JwtToken jwtToken = refreshTokenService.issue(authentication.getName());
            refreshTokenCookieManager.write(
                    response,
                    jwtToken.getRefreshToken(),
                    jwtToken.getRefreshTokenExpiresAt()
            );
            JsonResponse.send(response, TokenResponse.from(jwtToken));
        });
        setAuthenticationFailureHandler((request, response, exception) -> {
            boolean invalidRequest = exception instanceof InvalidLoginRequestException;
            response.setStatus(invalidRequest
                    ? HttpStatus.BAD_REQUEST.value()
                    : HttpStatus.UNAUTHORIZED.value());
            JsonResponse.send(response, new MessageResponse(invalidRequest
                    ? "로그인 요청 형식이 올바르지 않습니다."
                    : "이메일 또는 비밀번호가 올바르지 않습니다."));
        });
    }

    // 로그인 JSON을 읽고 인증 검증을 AuthenticationManager에 위임
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequest login = LoginRequest.of(request);
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(login.getEmail(), login.getPassword());
            return getAuthenticationManager().authenticate(token);
        } catch (IOException e) {
            throw new InvalidLoginRequestException("로그인 요청 형식이 올바르지 않습니다.", e);
        }
    }
}
