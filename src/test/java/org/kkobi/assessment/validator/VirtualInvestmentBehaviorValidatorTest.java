package org.kkobi.assessment.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorRequest;
import org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualInvestmentBehaviorValidatorTest {

    private final VirtualInvestmentBehaviorValidator validator =
            new VirtualInvestmentBehaviorValidator(new ValidVirtualInvestmentBehaviorMapper());

    @Test
    @DisplayName("증권 행동은 주가 등락률이 필수이다.")
    void validateSecurityBehaviorRequiresCurrentPriceChangeRate() {
        VirtualInvestmentBehaviorRequest request = createSecurityRequest();
        request.setCurrentPriceChangeRate(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateVirtualInvestmentBehavior(request)
        );
    }

    @Test
    @DisplayName("증권 행동은 당일 변동률이 필수이다.")
    void validateSecurityBehaviorRequiresDailyPriceRangeRate() {
        VirtualInvestmentBehaviorRequest request = createSecurityRequest();
        request.setDailyPriceRangeRate(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateVirtualInvestmentBehavior(request)
        );
    }

    @Test
    @DisplayName("예적금 행동은 주가 정보 없이 검증한다.")
    void validateProductBehaviorWithoutSecurityPriceRates() {
        List.of("JOIN_PRODUCTS", "CANCEL_PRODUCTS", "MATURITY")
                .forEach(actionType -> assertDoesNotThrow(() ->
                        validator.validateVirtualInvestmentBehavior(
                                createProductRequest(actionType)
                        )
                ));
    }

    @Test
    @DisplayName("증권 행동은 증권 주문 원본 거래 유형만 허용한다.")
    void validateSecurityBehaviorReferenceType() {
        VirtualInvestmentBehaviorRequest request = createSecurityRequest();
        request.setReferenceType("PRODUCT_TRANSACTION");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateVirtualInvestmentBehavior(request)
        );
    }

    @Test
    @DisplayName("예적금 행동은 상품 거래 원본 거래 유형만 허용한다.")
    void validateProductBehaviorReferenceType() {
        VirtualInvestmentBehaviorRequest request = createProductRequest("JOIN_PRODUCTS");
        request.setReferenceType("SECURITY_ORDER");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateVirtualInvestmentBehavior(request)
        );
    }

    @Test
    @DisplayName("원본 거래 ID는 0보다 커야 한다.")
    void validatePositiveReferenceId() {
        VirtualInvestmentBehaviorRequest request = createSecurityRequest();
        request.setReferenceId(0L);

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateVirtualInvestmentBehavior(request)
        );
    }

    private VirtualInvestmentBehaviorRequest createSecurityRequest() {
        VirtualInvestmentBehaviorRequest request = createRequiredRequest();
        request.setActionType("BUY");
        request.setAssetType("SECURITY");
        request.setReferenceType("SECURITY_ORDER");
        request.setReferenceId(1L);
        request.setSecurityId(1L);
        request.setStockCode("TEST");
        request.setQuantity(1);
        request.setCurrentPriceChangeRate(BigDecimal.ZERO);
        request.setDailyPriceRangeRate(BigDecimal.ZERO);
        return request;
    }

    private VirtualInvestmentBehaviorRequest createProductRequest(String actionType) {
        VirtualInvestmentBehaviorRequest request = createRequiredRequest();
        request.setActionType(actionType);
        request.setAssetType("PRODUCTS");
        request.setReferenceType("PRODUCT_TRANSACTION");
        request.setReferenceId(1L);
        request.setProductOptionId(1L);
        return request;
    }

    private VirtualInvestmentBehaviorRequest createRequiredRequest() {
        VirtualInvestmentBehaviorRequest request = new VirtualInvestmentBehaviorRequest();
        request.setUserId(1L);
        request.setAccountId(1L);
        request.setActionAmount(100_000L);
        request.setCurrentCash(900_000L);
        request.setCurrentStockPrincipal(0L);
        request.setCurrentDeposit(100_000L);
        request.setTradedAt(LocalDateTime.of(2026, 8, 10, 9, 0));
        return request;
    }

    private static class ValidVirtualInvestmentBehaviorMapper
            implements VirtualInvestmentBehaviorMapper {

        @Override
        public boolean existsAccountByUserId(Long accountId, Long userId) {
            return true;
        }

        @Override
        public boolean existsSecurityByIdAndStockCode(Long securityId, String stockCode) {
            return true;
        }

        @Override
        public boolean existsProductOption(Long productOptionId) {
            return true;
        }

        @Override
        public VirtualInvestmentBehaviorRequest getProductBehaviorRequest(
                Long userId,
                Long productTransactionId) {
            return null;
        }

        @Override
        public List<VirtualInvestmentBehaviorDto> getPreviousVirtualInvestmentBehaviors(
                Long accountId,
                Timestamp tradedAt) {
            return List.of();
        }
    }
}
