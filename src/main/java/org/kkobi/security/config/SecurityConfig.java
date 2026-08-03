package org.kkobi.security.config;

import lombok.RequiredArgsConstructor;
import org.kkobi.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.kkobi.security.jwt.JwtAuthenticationFilter;
import org.kkobi.security.jwt.JwtProvider;
import org.kkobi.security.util.JsonResponse;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@MapperScan(basePackages = {
        "org.kkobi.users.mapper"
})
@ComponentScan(basePackages = {
        "org.kkobi.security",
        "org.kkobi.users.service"
})
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtProvider jwtProvider;

    // Spring Security 필터보다 먼저 적용할 UTF-8 인코딩 필터를 생성
    public CharacterEncodingFilter encodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }

    // JWT 필터, 예외 처리, URL 인증 규칙, 무상태 세션을 설정
    @Override
    public void configure(HttpSecurity http) throws Exception {
        JwtUsernamePasswordAuthenticationFilter loginFilter =
                new JwtUsernamePasswordAuthenticationFilter(authenticationManagerBean(), jwtProvider);

        http
                .addFilterBefore(encodingFilter(), CsrfFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) ->
                        JsonResponse.sendError(response, HttpStatus.UNAUTHORIZED, authException.getMessage()))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        JsonResponse.sendError(response, HttpStatus.FORBIDDEN, "Access denied"))
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS).permitAll()
                .antMatchers("/", "/resources/**", "/error").permitAll()
                .antMatchers("/api/auth/login", "/api/auth/signup").permitAll()
                .antMatchers("/api/security/all").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic().disable()
                .csrf().disable()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    // 로그인 인증에 사용할 사용자 조회 서비스와 비밀번호 인코더를 등록
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    // Spring Security에서 사용할 BCrypt 비밀번호 인코더를 제공
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 로그인 필터가 아이디와 비밀번호를 검증할 수 있도록 AuthenticationManager를 빈으로 노출
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    // 브라우저 클라이언트의 API 요청을 허용하는 CORS 필터를 생성
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    // 정적 리소스는 Spring Security 필터를 거치지 않도록 제외
    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/assets/**");
    }
}
