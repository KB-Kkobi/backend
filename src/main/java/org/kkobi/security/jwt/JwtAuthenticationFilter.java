package org.kkobi.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.kkobi.security.util.JsonResponse;
import org.kkobi.security.util.JwtProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProcessor jwtProcessor;
    private final UserDetailsService userDetailsService;

    // 유효한 JWT로 Spring Security 인증 객체를 생성
    private Authentication getAuthentication(String token) {
        String username = jwtProcessor.getUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    // Bearer 토큰을 추출해 검증하고 인증 정보를 SecurityContext에 저장
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

            if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
                String token = bearerToken.substring(BEARER_PREFIX.length());
                if (jwtProcessor.validateToken(token)) {
                    SecurityContextHolder.getContext().setAuthentication(getAuthentication(token));
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, "Token has expired");
        } catch (JwtException | IllegalArgumentException e) {
            JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }
}
