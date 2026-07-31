package org.kkobi.product.saving.controller;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.saving.service.SavingProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
