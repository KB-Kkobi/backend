package org.kkobi.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.account.dto.AccountAssetStatusResponseDto;
import org.kkobi.account.service.AccountService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
@Tag(name = "계좌", description = "계좌 생성 API")
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "가상투자 계좌 생성",
            description = "로그인 사용자의 가상투자 계좌를 초기 투자금 500만 원으로 생성합니다."
    )
    @PostMapping
    public ResponseEntity<Void> createAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        accountService.createAccount(
                authenticatedUser.getUserId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @Operation(
            summary = "가상투자 계좌 자산 조회",
            description = "로그인 사용자의 현금, 주식, 예적금 자산과 비중을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<AccountAssetStatusResponseDto> getAccountAssetStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticatedUser
    ) {
        AccountAssetStatusResponseDto response =
                accountService.getAccountAssetStatus(
                        authenticatedUser.getUserId()
                );
        return ResponseEntity.ok(response);
    }
}
