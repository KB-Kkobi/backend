package org.kkobi.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.account.dto.AccountCreateRequestDto;
import org.kkobi.account.service.AccountService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
@Tag(name = "계좌", description = "계좌 생성 API")
public class AccountController {

    private final AccountService accountService;

    // 가상투자 최초 시작 시 로그인한 사용자의 계좌 생성
    @Operation(
            summary = "계좌 생성",
            description = "로그인한 사용자의 투자 계좌를 생성합니다."
    )
    @PostMapping
    public ResponseEntity<Void> createAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser,
            @RequestBody AccountCreateRequestDto request
            ) {
        accountService.createAccount(
                authenticatedUser.getUserId(),
                request
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }
}
