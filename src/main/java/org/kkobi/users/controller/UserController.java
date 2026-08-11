package org.kkobi.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.security.jwt.JwtToken;
import org.kkobi.security.token.RefreshTokenCookieManager;
import org.kkobi.security.token.RefreshTokenService;
import org.kkobi.users.dto.request.SignupRequest;
import org.kkobi.users.dto.response.MessageResponse;
import org.kkobi.users.dto.response.TokenResponse;
import org.kkobi.users.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// 회원가입 요청을 처리하는 REST 컨트롤러
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "회원", description = "회원 인증 및 가입 API")
public class UserController {

    // 회원가입 비즈니스 로직을 처리하는 서비스
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    // 회원가입 정보를 검증하고 새로운 사용자를 등록
    @Operation(
            summary = "회원가입",
            description = "사용자 정보를 입력받아 회원가입을 진행합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("회원가입이 완료되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        JwtToken token = refreshTokenService.reissue(refreshTokenCookieManager.read(request));
        refreshTokenCookieManager.write(response, token.getRefreshToken(), token.getRefreshTokenExpiresAt());
        return ResponseEntity.ok(TokenResponse.from(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        refreshTokenService.revoke(refreshTokenCookieManager.read(request));
        refreshTokenCookieManager.clear(response);
        return ResponseEntity.ok(new MessageResponse("로그아웃되었습니다."));
    }
}
