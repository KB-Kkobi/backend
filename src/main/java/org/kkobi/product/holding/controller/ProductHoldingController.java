package org.kkobi.product.holding.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.product.holding.dto.request.ProductSubscriptionRequestDto;
import org.kkobi.product.holding.dto.response.*;
import org.springframework.web.bind.annotation.*;
import org.kkobi.product.holding.service.ProductHoldingService;
import org.kkobi.security.principal.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/holdings")
@Tag(name = "예적금 보유", description ="예적금 가입 및 보유 상품 조회 API")
public class ProductHoldingController {

    private final ProductHoldingService productHoldingService;

    // 로그인 사용자가 예금 또는 적금 상품에 가입
    @Operation(
            summary = "예적금 상품 가입",
            description = "선택한 예금 또는 적금 상품에 가입합니다."
    )
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

    @Operation(
            summary = "예적금 가입 예상 조회",
            description = "가입 조건에 따른 세후 예상 만기금액을 조회합니다."
    )
    @PostMapping("/subscription-estimate")
    public ResponseEntity<ProductSubscriptionEstimateResponseDto> estimateSubscription(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser,
            @Valid @RequestBody ProductSubscriptionRequestDto request
    ) {
        ProductSubscriptionEstimateResponseDto response =
                productHoldingService.estimateSubscription(
                        authenticateUser.getUserId(),
                        request
                );
        return ResponseEntity.ok(response);
    }

    // 로그인 사용자의 보유 예적금을 해지
    @Operation(
            summary = "예적금 상품 해지",
            description = "로그인한 사용자가 보유 중인 예금 또는 적금 상품을 해지합니다."
    )
    @PostMapping("/{holdingProductId}/terminate")
    public ResponseEntity<ProductTerminationResponseDto> terminateProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser,
            @PathVariable Long holdingProductId
    ) {
        ProductTerminationResponseDto response =
                productHoldingService.terminateProduct(
                        authenticateUser.getUserId(),
                        holdingProductId
                );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "예적금 해지 예상 조회",
            description = "실제 해지 없이 만기 유지와 현재 해지 결과를 비교 조회합니다."
    )
    @GetMapping("/{holdingProductId}/termination-estimate")
    public ResponseEntity<ProductTerminationEstimateResponseDto> getTerminationEstimate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser,
            @PathVariable Long holdingProductId
    ) {
        ProductTerminationEstimateResponseDto response =
                productHoldingService.getTerminationEstimate(
                        authenticateUser.getUserId(),
                        holdingProductId
                );
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "보유 예적금 단건 조회",
            description = "로그인 사용자가 보유한 예금 또는 적금의 상세 정보를 조회합니다."
    )
    @GetMapping("/{holdingProductId}")
    public ResponseEntity<ProductHoldingListItemResponseDto> getHoldingProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser,
            @PathVariable Long holdingProductId
    ) {
        ProductHoldingListItemResponseDto response =
                productHoldingService.getHoldingProductDetail(
                        authenticateUser.getUserId(),
                        holdingProductId
                );
        return ResponseEntity.ok(response);
    }

    // 로그인 사용자의 보유 예적금 목록 조회
    @Operation(
            summary = "보유 예적금 조회",
            description = "로그인한 사용자가 보유한 예금 및 적금 상품을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<List<ProductHoldingListItemResponseDto>> getHoldingProducts (
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser
    ){

        List<ProductHoldingListItemResponseDto> response = productHoldingService.getHoldingProducts(
                authenticateUser.getUserId());

        return ResponseEntity.ok(response);
    }

    //로그인 사용자의 전체 저축 자산 현황 조회
    @GetMapping("/assets")
    public ResponseEntity<SavingsAssetStatusResponseDto> getSavingsAssetStatus(
            @AuthenticationPrincipal CustomUserDetails authenticateUser
    ){
        SavingsAssetStatusResponseDto response =
                productHoldingService.getSavingAssetStatus(authenticateUser.getUserId());

        return ResponseEntity.ok(response);
    }

    // 로그인 사용자의 예적금 해지 이력 조회
    @Operation(
            summary = "예적금 해지 이력 조회",
            description = "로그인한 사용자의 예금 및 적금 해지 이력을 조회합니다."
    )
    @GetMapping("/history")
    public ResponseEntity<List<ProductHoldingTransactionHistoryResponseDto>> getProductHoldingHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails authenticateUser
    ) {
        List<ProductHoldingTransactionHistoryResponseDto> response =
                productHoldingService.getProductHoldingHistory(
                        authenticateUser.getUserId()
                );

        return ResponseEntity.ok(response);
    }
}
