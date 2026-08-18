package org.kkobi.product.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.product.deposit.service.DepositProductService;
import org.kkobi.product.dto.request.ProductListRequestDto;
import org.kkobi.product.dto.response.ProductListItemResponseDto;
import org.kkobi.product.dto.response.ProductListResponseDto;
import org.kkobi.product.saving.service.SavingProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ProductRecommendationService {

    private final DepositProductService depositProductService;
    private final SavingProductService savingProductService;

    // 홈 화면 추천 예·적금 1건 조회 (예금·적금 각각 최고 우대금리 1위 중 더 높은 쪽)
    @Transactional(readOnly = true)
    public ProductListItemResponseDto getRecommendedProduct() {

        ProductListItemResponseDto topDeposit =
                getTopProduct(depositProductService::getDepositProductList);
        ProductListItemResponseDto topSaving =
                getTopProduct(savingProductService::getSavingProductList);

        if (topDeposit == null) {
            return topSaving;
        }
        if (topSaving == null) {
            return topDeposit;
        }

        BigDecimal depositRate = topDeposit.getMaximumInterestRate();
        BigDecimal savingRate = topSaving.getMaximumInterestRate();

        if (depositRate == null) {
            return topSaving;
        }
        if (savingRate == null) {
            return topDeposit;
        }

        return depositRate.compareTo(savingRate) >= 0 ? topDeposit : topSaving;
    }

    // 상품 목록 조회 함수로 최고 우대금리 1위 상품을 조회 (없으면 null)
    private ProductListItemResponseDto getTopProduct(
            Function<ProductListRequestDto, ProductListResponseDto> fetchList) {

        ProductListRequestDto request = new ProductListRequestDto();
        request.setPage(1);
        request.setSize(1);
        request.setSort("maximumInterestRate,desc");

        List<ProductListItemResponseDto> content = fetchList.apply(request).getContent();

        return content.isEmpty() ? null : content.get(0);
    }
}
