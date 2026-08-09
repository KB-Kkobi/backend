package org.kkobi.product.deposit.controller;

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
public class DepositProductController {

    // 예금 상품 조회 및 수집 로직을 처리하는 Service
    private final DepositProductService depositProductService;

    // 금융감독원 예금 상품 API 조회
    @GetMapping("/external")
    public DepositApiResponse getDepositProducts(
            @RequestParam(defaultValue = "1") int pageNumber) {

        return depositProductService.getDepositProducts(pageNumber);
    }

    // 금융감독원 예금 상품 데이터를 조회하여 DB에 저장
    @PostMapping(value = "/collect")
    public ResponseEntity<Void> collectDepositProducts() {

        depositProductService.collectDepositProducts();

        return ResponseEntity.ok().build();
    }

    // 상품 ID로 예금 상품 상세 정보 조회
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
    @GetMapping
    public ResponseEntity<ProductListResponseDto> getDepositProductList(
            @ModelAttribute ProductListRequestDto request
    ){
        return ResponseEntity.ok(
                depositProductService.getDepositProductList(request)
        );
    }
}
