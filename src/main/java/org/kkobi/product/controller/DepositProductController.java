package org.kkobi.product.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.dto.DepositApiResponse;
import org.kkobi.product.service.DepositProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/deposits")
public class DepositProductController {

    // 예금 상품 조회 및 수집 로직을 처리하는 Service
    private final DepositProductService depositProductService;

    // 금융감독원 API에서 예금 상품 정보를 조회한다.
    @GetMapping("/external")
    public DepositApiResponse getDepositProducts(
            @RequestParam(defaultValue = "1") int pageNumber) {

        return depositProductService.getDepositProducts(pageNumber);
    }
}
