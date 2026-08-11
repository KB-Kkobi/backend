package org.kkobi.product.holding.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.account.dto.AccountAssetInfoDto;
import org.kkobi.account.mapper.AccountMapper;
import org.kkobi.product.holding.dto.ProductHoldingInfoDto;
import org.kkobi.product.holding.dto.ProductSubscriptionInfoDto;
import org.kkobi.product.holding.dto.request.ProductSubscriptionRequestDto;
import org.kkobi.product.holding.dto.response.ProductSubscriptionEstimateResponseDto;
import org.kkobi.product.holding.dto.response.ProductTerminationEstimateResponseDto;
import org.kkobi.product.holding.dto.response.ProductTerminationResponseDto;
import org.kkobi.product.mapper.ProductHoldingMapper;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductHoldingServiceTest {

    @Test
    @DisplayName("해지 예상 조회와 실제 해지는 동일한 반환금액을 사용한다")
    void reuseTerminationCalculation() {
        ProductHoldingInfoDto holding = createDepositHolding();
        AccountAssetInfoDto account = createAccount();
        AtomicInteger terminationUpdateCount = new AtomicInteger();

        ProductHoldingMapper productMapper = createProductMapper(
                holding,
                null,
                terminationUpdateCount
        );
        ProductHoldingService service = new ProductHoldingService(
                productMapper,
                createAccountMapper(account)
        );

        ProductTerminationEstimateResponseDto estimate =
                service.getTerminationEstimate(10L, 1L);
        ProductTerminationResponseDto termination =
                service.terminateProduct(10L, 1L);

        assertEquals(
                0,
                estimate.getTerminationRefundAmount()
                        .compareTo(termination.getRefundAmount())
        );
        assertTrue(estimate.getForegoneInterest()
                .compareTo(BigDecimal.ZERO) > 0);
        assertTrue(estimate.getCurrentSavingsRatio()
                .compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, BigDecimal.ZERO.compareTo(
                estimate.getAfterTerminationSavingsRatio()
        ));
        assertEquals(4, terminationUpdateCount.get());
    }

    @Test
    @DisplayName("가입 예상 조회는 적금의 세후 만기 예상금액을 서버에서 계산한다")
    void estimateSavingSubscription() {
        ProductSubscriptionInfoDto subscriptionInfo =
                new ProductSubscriptionInfoDto();
        subscriptionInfo.setAccountId(1L);
        subscriptionInfo.setCashBalance(new BigDecimal("10000000"));
        subscriptionInfo.setProductOptionId(20L);
        subscriptionInfo.setProductType("SAVING");
        subscriptionInfo.setFinancialCompanyName("국민은행");
        subscriptionInfo.setProductName("테스트 적금");
        subscriptionInfo.setSavingTerm(12);
        subscriptionInfo.setInterestRate(new BigDecimal("3.00"));
        subscriptionInfo.setMaximumInterestRate(new BigDecimal("4.00"));

        ProductSubscriptionRequestDto request =
                new ProductSubscriptionRequestDto();
        request.setProductOptionId(20L);
        request.setJoinAmount(new BigDecimal("200000"));
        request.setPreferentialRateApplied(true);
        request.setPaymentDay(20);

        ProductHoldingService service = new ProductHoldingService(
                createProductMapper(
                        null,
                        subscriptionInfo,
                        new AtomicInteger()
                ),
                createAccountMapper(createAccount())
        );

        ProductSubscriptionEstimateResponseDto response =
                service.estimateSubscription(10L, request);

        assertEquals(0, new BigDecimal("2400000")
                .compareTo(response.getExpectedPrincipal()));
        assertTrue(response.getExpectedAfterTaxInterest()
                .compareTo(BigDecimal.ZERO) > 0);
        assertTrue(response.getExpectedMaturityAmount()
                .compareTo(response.getExpectedPrincipal()) > 0);
    }

    private ProductHoldingInfoDto createDepositHolding() {
        LocalDate today = LocalDate.now();
        ProductHoldingInfoDto holding = new ProductHoldingInfoDto();
        holding.setHoldingProductId(1L);
        holding.setAccountId(1L);
        holding.setProductOptionId(10L);
        holding.setProductType("DEPOSIT");
        holding.setFinancialCompanyName("국민은행");
        holding.setProductName("테스트 예금");
        holding.setJoinAmount(new BigDecimal("2000000"));
        holding.setAppliedRate(new BigDecimal("3.10"));
        holding.setSavingTerm(12);
        holding.setStartDate(today.minusMonths(2));
        holding.setMaturityDate(today.plusMonths(10));
        holding.setStatus("ACTIVE");
        return holding;
    }

    private AccountAssetInfoDto createAccount() {
        AccountAssetInfoDto account = new AccountAssetInfoDto();
        account.setAccountId(1L);
        account.setSeedMoney(new BigDecimal("10000000"));
        account.setMonthlyInvestAmount(new BigDecimal("300000"));
        account.setCashBalance(new BigDecimal("8000000"));
        account.setStockAsset(BigDecimal.ZERO);
        return account;
    }

    private ProductHoldingMapper createProductMapper(
            ProductHoldingInfoDto holding,
            ProductSubscriptionInfoDto subscriptionInfo,
            AtomicInteger updateCount
    ) {
        return (ProductHoldingMapper) Proxy.newProxyInstance(
                ProductHoldingMapper.class.getClassLoader(),
                new Class<?>[]{ProductHoldingMapper.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getHoldingProductForTermination" -> holding;
                    case "getHoldingProductsByUserId" ->
                            holding == null ? List.of() : List.of(holding);
                    case "getProductSubscriptionInfo" -> subscriptionInfo;
                    case "terminateHoldingProduct",
                            "increaseAccountCashBalance",
                            "saveProductTerminationTransaction",
                            "saveAccountDepositTransaction" -> {
                        updateCount.incrementAndGet();
                        yield 1;
                    }
                    case "getAnnualInterestIncome" -> BigDecimal.ZERO;
                    case "getProductHoldingHistory" -> List.of();
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private AccountMapper createAccountMapper(AccountAssetInfoDto account) {
        return (AccountMapper) Proxy.newProxyInstance(
                AccountMapper.class.getClassLoader(),
                new Class<?>[]{AccountMapper.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAccountAssetInfoByUserId" -> account;
                    case "existsAccountByUserId" -> account != null;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == int.class) return 0;
        if (returnType == boolean.class) return false;
        return null;
    }
}
