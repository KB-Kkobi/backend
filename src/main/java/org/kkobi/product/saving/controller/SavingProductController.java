package org.kkobi.product.saving.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.dto.response.ProductDetailResponseDto;
import org.kkobi.product.saving.service.SavingProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/savings")
public class SavingProductController {

    private final SavingProductService savingProductService;

    // 금융감독원 적금 상품 데이터를 수집하여 DB에 저장
    @PostMapping(
            value = "/collect",
            produces = "text/plain;charset=UTF-8"
    )
    public ResponseEntity<String> collectSavingProducts() {

        savingProductService.collectSavingProducts();

        return ResponseEntity.ok("적금 상품 데이터 수집이 완료되었습니다");
    }

    // 상품 ID로 적금 상품 상세 정보 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDto> getSavingProductDetail(
            @PathVariable Long productId
    ) {
        // 적금 상품 상세 정보 반환
        return ResponseEntity.ok(
                savingProductService.getSavingProductDetail(productId)
        );
    }
}
