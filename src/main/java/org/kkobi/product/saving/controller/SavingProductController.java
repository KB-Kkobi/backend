package org.kkobi.product.saving.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.product.dto.request.ProductListRequestDto;
import org.kkobi.product.dto.response.ProductDetailResponseDto;
import org.kkobi.product.dto.response.ProductListResponseDto;
import org.kkobi.product.saving.service.SavingProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/savings")
@Tag(name = "적금 상품", description = "적금 상품 조회 API")
public class SavingProductController {

    private final SavingProductService savingProductService;

    // 금융감독원 적금 상품 데이터를 수집하여 DB에 저장
    @Operation(
            summary = "적금 상품 데이터 수집",
            description = "외부 적금 상품 데이터를 수집하여 저장합니다."
    )
    @PostMapping("/collect")
    public ResponseEntity<Void> collectSavingProducts() {

        savingProductService.collectSavingProducts();

        return ResponseEntity.ok().build();
    }

    // 상품 ID로 적금 상품 상세 정보 조회
    @Operation(
            summary = "적금 상품 상세 조회",
            description = "선택한 적금 상품의 기본 정보와 가입 기간별 금리 정보를 조회합니다."
    )
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDto> getSavingProductDetail(
            @PathVariable Long productId
    ) {
        // 적금 상품 상세 정보 반환
        return ResponseEntity.ok(
                savingProductService.getSavingProductDetail(productId)
        );
    }

    // 적금 상품 목록 조회
    @Operation(
            summary = "적금 상품 목록 조회",
            description = "검색, 가입 기간, 적립 방식, 금리 정렬 조건에 따라 적금 상품 목록을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ProductListResponseDto> getSavingProductList(
            @ModelAttribute ProductListRequestDto request
            ){
        return ResponseEntity.ok(
                savingProductService.getSavingProductList(request)
        );
    }

}
