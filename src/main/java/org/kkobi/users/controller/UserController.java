package org.kkobi.users.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.users.dto.request.SignupRequest;
import org.kkobi.users.dto.response.MessageResponse;
import org.kkobi.users.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

// 회원가입 요청을 처리하는 REST 컨트롤러
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    // 회원가입 비즈니스 로직을 처리하는 서비스
    private final UserService userService;

    // 회원가입 정보를 검증하고 새로운 사용자를 등록
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("회원가입이 완료되었습니다."));
    }
}
