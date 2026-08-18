package org.kkobi.product.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.product.deposit.service.DepositProductService;
import org.kkobi.product.dto.request.ProductListRequestDto;
import org.kkobi.product.dto.response.ProductListItemResponseDto;
import org.kkobi.product.dto.response.ProductListResponseDto;
import org.kkobi.product.saving.service.SavingProductService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductRecommendationServiceTest {

    @Test
    @DisplayName("예금 최고금리가 더 높으면 예금 상품을 추천한다")
    void recommendsDepositWhenDepositRateIsHigher() {
        DepositProductService depositProductService = mock(DepositProductService.class);
        SavingProductService savingProductService = mock(SavingProductService.class);

        when(depositProductService.getDepositProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse(product("DEPOSIT", "3.50")));
        when(savingProductService.getSavingProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse(product("SAVING", "3.00")));

        ProductRecommendationService service =
                new ProductRecommendationService(depositProductService, savingProductService);

        ProductListItemResponseDto result = service.getRecommendedProduct();

        assertEquals("DEPOSIT", result.getProductType());
        assertEquals(0, new BigDecimal("3.50").compareTo(result.getMaximumInterestRate()));
    }

    @Test
    @DisplayName("적금 최고금리가 더 높으면 적금 상품을 추천한다")
    void recommendsSavingWhenSavingRateIsHigher() {
        DepositProductService depositProductService = mock(DepositProductService.class);
        SavingProductService savingProductService = mock(SavingProductService.class);

        when(depositProductService.getDepositProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse(product("DEPOSIT", "3.00")));
        when(savingProductService.getSavingProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse(product("SAVING", "4.20")));

        ProductRecommendationService service =
                new ProductRecommendationService(depositProductService, savingProductService);

        ProductListItemResponseDto result = service.getRecommendedProduct();

        assertEquals("SAVING", result.getProductType());
        assertEquals(0, new BigDecimal("4.20").compareTo(result.getMaximumInterestRate()));
    }

    @Test
    @DisplayName("한쪽 상품이 없으면 있는 쪽을 그대로 추천한다")
    void fallsBackToTheOtherTypeWhenOneIsEmpty() {
        DepositProductService depositProductService = mock(DepositProductService.class);
        SavingProductService savingProductService = mock(SavingProductService.class);

        when(depositProductService.getDepositProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse());
        when(savingProductService.getSavingProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse(product("SAVING", "4.20")));

        ProductRecommendationService service =
                new ProductRecommendationService(depositProductService, savingProductService);

        ProductListItemResponseDto result = service.getRecommendedProduct();

        assertEquals("SAVING", result.getProductType());
    }

    @Test
    @DisplayName("예금·적금이 모두 없으면 null을 반환한다")
    void returnsNullWhenBothAreEmpty() {
        DepositProductService depositProductService = mock(DepositProductService.class);
        SavingProductService savingProductService = mock(SavingProductService.class);

        when(depositProductService.getDepositProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse());
        when(savingProductService.getSavingProductList(any(ProductListRequestDto.class)))
                .thenReturn(listResponse());

        ProductRecommendationService service =
                new ProductRecommendationService(depositProductService, savingProductService);

        assertNull(service.getRecommendedProduct());
    }

    private ProductListItemResponseDto product(String productType, String maximumInterestRate) {
        ProductListItemResponseDto dto = new ProductListItemResponseDto();
        dto.setProductType(productType);
        dto.setMaximumInterestRate(new BigDecimal(maximumInterestRate));
        return dto;
    }

    private ProductListResponseDto listResponse(ProductListItemResponseDto... items) {
        ProductListResponseDto response = new ProductListResponseDto();
        response.setContent(List.of(items));
        return response;
    }
}
