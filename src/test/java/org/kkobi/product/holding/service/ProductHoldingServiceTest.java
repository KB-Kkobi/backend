package org.kkobi.product.holding.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.account.dto.AccountAssetInfoDto;
import org.kkobi.account.mapper.AccountMapper;
import org.kkobi.assessment.service.VirtualInvestmentAssessmentService;
import org.kkobi.product.holding.dto.PreferentialRateConditionInfoDto;
import org.kkobi.product.holding.dto.ProductHoldingInfoDto;
import org.kkobi.product.holding.dto.ProductSubscriptionInfoDto;
import org.kkobi.product.holding.dto.request.ProductSubscriptionRequestDto;
import org.kkobi.product.holding.dto.response.ProductSubscriptionEstimateResponseDto;
import org.kkobi.product.holding.dto.response.ProductSubscriptionResponseDto;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProductHoldingServiceTest {

    @Test
    @DisplayName("해지 예상 조회와 실제 해지는 동일한 반환금액을 사용한다")
    void reuseTerminationCalculation() {
        ProductHoldingInfoDto holding = createDepositHolding();
        AccountAssetInfoDto account = createAccount();
        AtomicInteger terminationUpdateCount = new AtomicInteger();
        VirtualInvestmentAssessmentService assessmentService =
                mock(VirtualInvestmentAssessmentService.class);

        ProductHoldingMapper productMapper = createProductMapper(
                holding,
                null,
                terminationUpdateCount
        );

        ProductHoldingService service = new ProductHoldingService(
                productMapper,
                createAccountMapper(account),
                assessmentService
        );

        ProductTerminationEstimateResponseDto estimate =
                service.getTerminationEstimate(10L, 1L);

        ProductTerminationResponseDto termination =
                service.terminateProduct(10L, 1L);

        verify(assessmentService)
                .updateProductTransactionAssessment(10L, 100L);

        assertEquals(
                0,
                estimate.getTerminationRefundAmount()
                        .compareTo(termination.getRefundAmount())
        );

        assertTrue(
                estimate.getForegoneInterest()
                        .compareTo(BigDecimal.ZERO) > 0
        );

        assertTrue(
                estimate.getCurrentSavingsRatio()
                        .compareTo(BigDecimal.ZERO) > 0
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        estimate.getAfterTerminationSavingsRatio()
                )
        );

        assertEquals(4, terminationUpdateCount.get());
    }

    @Test
    @DisplayName("가입 예상 조회는 적금의 세후 만기 예상금액을 서버에서 계산한다")
    void estimateSavingSubscription() {
        ProductSubscriptionInfoDto subscriptionInfo =
                new ProductSubscriptionInfoDto();

        subscriptionInfo.setAccountId(1L);
        subscriptionInfo.setCashBalance(
                new BigDecimal("10000000")
        );
        subscriptionInfo.setProductOptionId(20L);
        subscriptionInfo.setProductType("SAVING");
        subscriptionInfo.setFinancialCompanyName("국민은행");
        subscriptionInfo.setProductName("테스트 적금");
        subscriptionInfo.setSavingTerm(12);
        subscriptionInfo.setInterestRate(
                new BigDecimal("3.00")
        );
        subscriptionInfo.setMaximumInterestRate(
                new BigDecimal("4.00")
        );

        ProductSubscriptionRequestDto request =
                new ProductSubscriptionRequestDto();

        request.setProductOptionId(20L);
        request.setJoinAmount(
                new BigDecimal("200000")
        );

        // 우대조건을 선택하지 않은 기본 가입 요청
        request.setSelectedPreferentialRateConditionIds(
                List.of()
        );

        request.setPaymentDay(20);

        ProductHoldingService service =
                new ProductHoldingService(
                        createProductMapper(
                                null,
                                subscriptionInfo,
                                new AtomicInteger()
                        ),
                        createAccountMapper(
                                createAccount()
                        ),
                        mock(
                                VirtualInvestmentAssessmentService.class
                        )
                );

        ProductSubscriptionEstimateResponseDto response =
                service.estimateSubscription(
                        10L,
                        request
                );

        assertEquals(
                0,
                new BigDecimal("2400000")
                        .compareTo(
                                response.getExpectedPrincipal()
                        )
        );

        assertTrue(
                response.getExpectedAfterTaxInterest()
                        .compareTo(BigDecimal.ZERO) > 0
        );

        assertTrue(
                response.getExpectedMaturityAmount()
                        .compareTo(
                                response.getExpectedPrincipal()
                        ) > 0
        );
    }

    @Test
    @DisplayName("예적금 가입 거래 저장 후 성향 점수를 재산정한다")
    void updateAssessmentAfterSubscription() {
        ProductSubscriptionInfoDto subscriptionInfo =
                new ProductSubscriptionInfoDto();

        subscriptionInfo.setAccountId(1L);
        subscriptionInfo.setCashBalance(
                new BigDecimal("10000000")
        );
        subscriptionInfo.setProductOptionId(20L);
        subscriptionInfo.setProductType("DEPOSIT");
        subscriptionInfo.setFinancialCompanyName(
                "테스트 은행"
        );
        subscriptionInfo.setProductName(
                "테스트 예금"
        );
        subscriptionInfo.setSavingTerm(12);
        subscriptionInfo.setInterestRate(
                new BigDecimal("3.00")
        );
        subscriptionInfo.setMaximumInterestRate(
                new BigDecimal("3.00")
        );

        ProductSubscriptionRequestDto request =
                new ProductSubscriptionRequestDto();

        request.setProductOptionId(20L);
        request.setJoinAmount(
                new BigDecimal("3000000")
        );

        // 우대조건을 선택하지 않은 기본 가입 요청
        request.setSelectedPreferentialRateConditionIds(
                List.of()
        );

        VirtualInvestmentAssessmentService assessmentService =
                mock(
                        VirtualInvestmentAssessmentService.class
                );

        ProductHoldingService service =
                new ProductHoldingService(
                        createProductMapper(
                                null,
                                subscriptionInfo,
                                new AtomicInteger()
                        ),
                        createAccountMapper(
                                createAccount()
                        ),
                        assessmentService
                );

        ProductSubscriptionResponseDto response =
                service.subscribeProduct(
                        10L,
                        request
                );

        assertEquals(
                20L,
                response.getProductOptionId()
        );

        verify(assessmentService)
                .updateProductTransactionAssessment(
                        10L,
                        100L
                );
    }

    @Test
    @DisplayName("선택한 우대조건의 추가금리를 합산해 최종 적용금리를 계산한다")
    void calculateAppliedRateWithSelectedConditions() {
        ProductSubscriptionInfoDto subscriptionInfo =
                new ProductSubscriptionInfoDto();

        subscriptionInfo.setAccountId(1L);
        subscriptionInfo.setCashBalance(
                new BigDecimal("10000000")
        );
        subscriptionInfo.setProductOptionId(20L);
        subscriptionInfo.setProductType("DEPOSIT");
        subscriptionInfo.setFinancialCompanyName(
                "테스트 은행"
        );
        subscriptionInfo.setProductName(
                "테스트 예금"
        );
        subscriptionInfo.setSavingTerm(12);

        // 기본금리 2.20%, 최고금리 3.55%
        subscriptionInfo.setInterestRate(
                new BigDecimal("2.20")
        );
        subscriptionInfo.setMaximumInterestRate(
                new BigDecimal("3.55")
        );

        ProductSubscriptionRequestDto request =
                new ProductSubscriptionRequestDto();

        request.setProductOptionId(20L);
        request.setJoinAmount(
                new BigDecimal("1000000")
        );

        // 0.10%p, 0.75%p 우대조건 선택
        request.setSelectedPreferentialRateConditionIds(
                List.of(1L, 2L)
        );

        PreferentialRateConditionInfoDto firstCondition =
                createPreferentialRateCondition(
                        1L,
                        20L,
                        "0.10"
                );

        PreferentialRateConditionInfoDto secondCondition =
                createPreferentialRateCondition(
                        2L,
                        20L,
                        "0.75"
                );

        ProductHoldingService service =
                new ProductHoldingService(
                        createProductMapper(
                                null,
                                subscriptionInfo,
                                new AtomicInteger(),
                                List.of(
                                        firstCondition,
                                        secondCondition
                                )
                        ),
                        createAccountMapper(
                                createAccount()
                        ),
                        mock(
                                VirtualInvestmentAssessmentService.class
                        )
                );

        ProductSubscriptionEstimateResponseDto response =
                service.estimateSubscription(
                        10L,
                        request
                );

        // 기본 2.20 + 우대 0.10 + 0.75 = 3.05
        assertEquals(
                0,
                new BigDecimal("3.05")
                        .compareTo(
                                response.getAppliedRate()
                        )
        );
    }

    // 테스트용 예금 보유 정보를 생성
    private ProductHoldingInfoDto createDepositHolding() {
        LocalDate today = LocalDate.now();

        ProductHoldingInfoDto holding =
                new ProductHoldingInfoDto();

        holding.setHoldingProductId(1L);
        holding.setAccountId(1L);
        holding.setProductOptionId(10L);
        holding.setProductType("DEPOSIT");
        holding.setFinancialCompanyName("국민은행");
        holding.setProductName("테스트 예금");
        holding.setJoinAmount(
                new BigDecimal("2000000")
        );
        holding.setAppliedRate(
                new BigDecimal("3.10")
        );
        holding.setSavingTerm(12);
        holding.setStartDate(
                today.minusMonths(2)
        );
        holding.setMaturityDate(
                today.plusMonths(10)
        );
        holding.setStatus("ACTIVE");

        return holding;
    }

    // 테스트용 계좌 자산 정보를 생성
    private AccountAssetInfoDto createAccount() {
        AccountAssetInfoDto account =
                new AccountAssetInfoDto();

        account.setAccountId(1L);
        account.setSeedMoney(
                new BigDecimal("10000000")
        );
        account.setCashBalance(
                new BigDecimal("8000000")
        );
        account.setStockAsset(
                BigDecimal.ZERO
        );

        return account;
    }

    // 기존 테스트에서 사용하는 ProductHoldingMapper 생성
    private ProductHoldingMapper createProductMapper(
            ProductHoldingInfoDto holding,
            ProductSubscriptionInfoDto subscriptionInfo,
            AtomicInteger updateCount
    ) {
        return createProductMapper(
                holding,
                subscriptionInfo,
                updateCount,
                List.of()
        );
    }

    // 우대조건 조회 결과를 포함한 ProductHoldingMapper 생성
    private ProductHoldingMapper createProductMapper(
            ProductHoldingInfoDto holding,
            ProductSubscriptionInfoDto subscriptionInfo,
            AtomicInteger updateCount,
            List<PreferentialRateConditionInfoDto> conditions
    ) {
        return (ProductHoldingMapper)
                Proxy.newProxyInstance(
                        ProductHoldingMapper.class
                                .getClassLoader(),
                        new Class<?>[]{
                                ProductHoldingMapper.class
                        },
                        (proxy, method, arguments) ->
                                switch (method.getName()) {

                                    case "getHoldingProductForTermination" ->
                                            holding;

                                    case "getHoldingProductsByUserId" ->
                                            holding == null
                                                    ? List.of()
                                                    : List.of(holding);

                                    case "getProductSubscriptionInfo" ->
                                            subscriptionInfo;

                                    // 선택한 우대조건 목록 반환
                                    case "getSelectedPreferentialRateConditions" ->
                                            conditions;

                                    case "getLastInsertedProductTransactionId" ->
                                            100L;

                                    case "saveHoldingProduct" -> {
                                        org.kkobi.product.holding.dto.ProductHoldingCreateDto holdingProduct =
                                                (org.kkobi.product.holding.dto.ProductHoldingCreateDto)
                                                        arguments[0];

                                        holdingProduct
                                                .setHoldingProductId(
                                                        1L
                                                );

                                        updateCount
                                                .incrementAndGet();

                                        yield 1;
                                    }

                                    case "decreaseAccountCashBalance",
                                         "saveProductSubscriptionTransaction",
                                         "saveAccountWithdrawalTransaction" -> {
                                        updateCount
                                                .incrementAndGet();

                                        yield 1;
                                    }

                                    case "terminateHoldingProduct",
                                         "increaseAccountCashBalance",
                                         "saveProductTerminationTransaction",
                                         "saveAccountDepositTransaction" -> {
                                        updateCount
                                                .incrementAndGet();

                                        yield 1;
                                    }

                                    case "getAnnualInterestIncome" ->
                                            BigDecimal.ZERO;

                                    case "getProductHoldingHistory" ->
                                            List.of();

                                    default ->
                                            defaultValue(
                                                    method.getReturnType()
                                            );
                                }
                );
    }

    // 테스트용 우대조건 정보를 생성
    private PreferentialRateConditionInfoDto
    createPreferentialRateCondition(
            Long conditionId,
            Long productOptionId,
            String additionalRate
    ) {
        PreferentialRateConditionInfoDto condition =
                new PreferentialRateConditionInfoDto();

        condition.setPreferentialRateConditionId(
                conditionId
        );

        condition.setProductOptionId(
                productOptionId
        );

        condition.setAdditionalRate(
                new BigDecimal(
                        additionalRate
                )
        );

        condition.setSelectable(true);

        return condition;
    }

    // 테스트용 AccountMapper 생성
    private AccountMapper createAccountMapper(
            AccountAssetInfoDto account
    ) {
        return (AccountMapper)
                Proxy.newProxyInstance(
                        AccountMapper.class
                                .getClassLoader(),
                        new Class<?>[]{
                                AccountMapper.class
                        },
                        (proxy, method, arguments) ->
                                switch (method.getName()) {

                                    case "getAccountAssetInfoByUserId" ->
                                            account;

                                    case "existsAccountByUserId" ->
                                            account != null;

                                    default ->
                                            defaultValue(
                                                    method.getReturnType()
                                            );
                                }
                );
    }

    // Proxy 메서드의 기본 반환값 처리
    private Object defaultValue(
            Class<?> returnType
    ) {
        if (returnType == int.class) {
            return 0;
        }

        if (returnType == boolean.class) {
            return false;
        }

        return null;
    }
}