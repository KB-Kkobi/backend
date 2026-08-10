package org.kkobi.product.holding.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.kkobi.product.holding.dto.request.ProductSubscriptionRequestDto;
import org.kkobi.product.holding.dto.response.ProductSubscriptionResponseDto;
import org.kkobi.product.holding.dto.response.ProductHoldingListItemResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.kkobi.product.holding.service.ProductHoldingService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/holdings")
public class ProductHoldingController {

    private final ProductHoldingService productHoldingService;

    // 로그인 사용자가 예금 또는 적금 상품에 가입
    @PostMapping
    public ResponseEntity<ProductSubscriptionResponseDto> subscribeProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser,
            @Valid @RequestBody ProductSubscriptionRequestDto request) {
        ProductSubscriptionResponseDto response =
                productHoldingService.subscribeProduct(authenticateUser.getUserId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 로그인 사용자의 보유 예적금 목록 조회
    @GetMapping
    public ResponseEntity<List<ProductHoldingListItemResponseDto>> getHoldingProducts (
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser
    ){

        List<ProductHoldingListItemResponseDto> response = productHoldingService.getHoldingProducts(
                authenticateUser.getUserId());

        return ResponseEntity.ok(response);
    }
}
