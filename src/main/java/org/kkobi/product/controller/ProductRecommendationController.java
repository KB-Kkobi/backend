package org.kkobi.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.kkobi.product.dto.response.ProductListItemResponseDto;
import org.kkobi.product.service.ProductRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/recommendations")
@Tag(name = "예·적금 추천", description = "홈 화면 예·적금 추천 API")
public class ProductRecommendationController {

    private final ProductRecommendationService productRecommendationService;

    // 홈 화면 추천 예·적금 조회 (예금·적금 각각 최고 우대금리 1위 중 더 높은 쪽 1건)
    @Operation(
            summary = "홈 화면 추천 예·적금 조회",
            description = "예금·적금 각각 최고 우대금리 1위 상품 중 더 높은 금리의 상품 1건을 반환합니다."
    )
    @GetMapping
    public ResponseEntity<ProductListItemResponseDto> getRecommendedProduct() {
        return ResponseEntity.ok(productRecommendationService.getRecommendedProduct());
    }
}
