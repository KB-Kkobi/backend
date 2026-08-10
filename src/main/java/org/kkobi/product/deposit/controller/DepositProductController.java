package org.kkobi.product.deposit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.product.deposit.dto.DepositApiResponse;
import org.kkobi.product.deposit.service.DepositProductService;
import org.kkobi.product.dto.request.ProductListRequestDto;
import org.kkobi.product.dto.response.ProductDetailResponseDto;
import org.kkobi.product.dto.response.ProductListResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/deposits")
@Tag(name = "예금 상품", description = "예금 상품 조회 API")
public class DepositProductController {

    // 예금 상품 조회 및 수집 로직을 처리하는 Service
    private final DepositProductService depositProductService;

    // 금융감독원 예금 상품 API 조회
    @Operation(
            summary = "금감원 예금 상품 원본 조회",
            description = "금융감독원 금융상품 API를 호출하여 예금 상품 원본 데이터를 조회합니다."
    )
    @GetMapping("/external")
    public DepositApiResponse getDepositProducts(
            @RequestParam(defaultValue = "1") int pageNumber) {

        return depositProductService.getDepositProducts(pageNumber);
    }

    // 금융감독원 예금 상품 데이터를 조회하여 DB에 저장
    @Operation(
            summary = "예금 상품 데이터 수집",
            description = "외부 예금 상품 데이터를 수집하여 저장합니다."
    )
    @PostMapping(value = "/collect")
    public ResponseEntity<Void> collectDepositProducts() {

        depositProductService.collectDepositProducts();

        return ResponseEntity.ok().build();
    }

    // 상품 ID로 예금 상품 상세 정보 조회
    @Operation(
            summary = "예금 상품 상세 조회",
            description = "선택한 예금 상품의 기본 정보와 가입 기간별 금리 정보를 조회합니다."
    )
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDto> getDepositProductDetail(
            @PathVariable Long productId
    ){
        // 예금 상품 상세 정보 반환
        return ResponseEntity.ok(
                depositProductService.getDepostProductDetail(productId)
        );
    }

    // 예금 상품 목록 조회
    @Operation(
            summary = "예금 상품 목록 조회",
            description = "검색, 가입 기간, 금리 정렬 조건에 따라 예금 상품 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ProductListResponseDto> getDepositProductList(
            @ModelAttribute ProductListRequestDto request
    ){
        return ResponseEntity.ok(
                depositProductService.getDepositProductList(request)
        );
    }
}
