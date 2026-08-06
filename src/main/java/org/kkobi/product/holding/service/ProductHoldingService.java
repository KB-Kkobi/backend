package org.kkobi.product.holding.service;


import lombok.RequiredArgsConstructor;
import org.kkobi.product.holding.dto.ProductHoldingCreateDto;
import org.kkobi.product.holding.dto.ProductSubscriptionInfoDto;
import org.kkobi.product.holding.dto.request.ProductSubscriptionRequestDto;
import org.kkobi.product.holding.dto.response.ProductSubscriptionResponseDto;
import org.kkobi.product.mapper.ProductHoldingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ProductHoldingService {

    private static final String DEPOSIT = "DEPOSIT";
    private static final String SAVING = "SAVING";
    private static final String ACTIVE = "ACTIVE";

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private final ProductHoldingMapper productHoldingMapper;

    // 로그인 사용자가 예금 또는 적금 상품에 가입
    @Transactional
    public ProductSubscriptionResponseDto subscribeProduct(
            Long userId,
            ProductSubscriptionRequestDto request
    ) {
        validateUserId(userId);
        validateRequest(request);

        ProductSubscriptionInfoDto subscriptionInfo =
                productHoldingMapper.getProductSubscriptionInfo(
                        userId,
                        request.getProductOptionId()
                );

        validateSubscriptionInfo(subscriptionInfo);
        validateProductRequest(subscriptionInfo, request);

        BigDecimal appliedRate =
                calculateAppliedRate(subscriptionInfo, request);

        LocalDate startDate = LocalDate.now(KOREA_ZONE_ID);
        LocalDate maturityDate =
                startDate.plusMonths(subscriptionInfo.getSavingTerm());

        ProductHoldingCreateDto holdingProduct =
                createHoldingProduct(
                        subscriptionInfo,
                        request,
                        appliedRate,
                        startDate,
                        maturityDate
                );

        decreaseAccountCashBalance(
                subscriptionInfo.getAccountId(),
                request.getJoinAmount()
        );

        saveHoldingProduct(holdingProduct);

        Integer installmentNumber =
                SAVING.equals(subscriptionInfo.getProductType()) ? 1 : null;

        saveTransactions(
                subscriptionInfo.getAccountId(),
                holdingProduct.getHoldingProductId(),
                request.getJoinAmount(),
                installmentNumber
        );

        return createResponse(
                subscriptionInfo,
                holdingProduct
        );
    }

    // 로그인 사용자 정보 확인
    private void validateUserId(Long userId) {
        if(userId == null){
            throw new IllegalArgumentException("사용자 정보가 없습니다.");
        }
    }

    // 가입 요청 정보 확인
    private void validateRequest(ProductSubscriptionRequestDto request) {
        if(request == null) {
            throw new IllegalArgumentException("가입 요청 정보가 없습니다.");
        }

        if(request.getProductOptionId() == null) {
            throw new IllegalArgumentException("상품 옵션 ID는 필수입니다.");
        }

        BigDecimal joinAmount = request.getJoinAmount();

        if(joinAmount == null || joinAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("가입 금액은 1원 이상이어야 합니다.");
        }

        if(joinAmount.stripTrailingZeros().scale() > 0){
            throw new IllegalArgumentException("가입 금액은 원 단위로 입력해야 합니다.");
        }

        if(request.getPreferentialRateApplied() == null){
            throw new IllegalArgumentException("우대 금리 적용 여부는 필수입니다");
        }
    }

    // 상품 옵션과 사용자 계좌 존재 여부 확인
    private void validateSubscriptionInfo(ProductSubscriptionInfoDto subscriptionInfo){
        if(subscriptionInfo == null){
            throw new IllegalArgumentException("상품 옵션을 찾을 수 없습니다.");
        }

        if(subscriptionInfo.getAccountId() == null){
            throw new IllegalArgumentException(
                    "먼저 가상투자 계좌를 생성해야 합니다."
            );
        }

        if(subscriptionInfo.getSavingTerm() == null || subscriptionInfo.getSavingTerm() <= 0){
            throw new IllegalArgumentException("상품 가입 기간 정보가 올바르지 않습니다");
        }
    }

    // 예금과 적금의 가입 조건 확인
    private void validateProductRequest(
            ProductSubscriptionInfoDto subscriptionInfo,
            ProductSubscriptionRequestDto request
    ) {
        String productType = subscriptionInfo.getProductType();

        if(!DEPOSIT.equals(productType) && !SAVING.equals(productType)){
            throw new IllegalArgumentException("지원하지 않는 상품 유형입니다.");
        }

        if(DEPOSIT.equals(productType) && request.getPaymentDay() != null){
            throw new IllegalArgumentException("예금 상품은 납입일을 입력할 수 없습니다.");
        }

        if(SAVING.equals(productType) && request.getPaymentDay() == null) {
            throw new IllegalArgumentException("적금 상품은 월 납입일이 필수입니다.");
        }

        Integer paymentDay = request.getPaymentDay();

        if(paymentDay != null && (paymentDay < 1 || paymentDay > 28)){
            throw new IllegalArgumentException("납입일은 1일부터 28일까지 선택할 수 있습니다.");
        }

        BigDecimal maximumLimit = subscriptionInfo.getMaxLimit();

        if(maximumLimit != null && request.getJoinAmount().compareTo(maximumLimit) > 0){
            throw new IllegalArgumentException("상품의 최대 가입 한도를 초과했습니다.");
        }

        BigDecimal cashBalance = subscriptionInfo.getCashBalance();

        if(cashBalance == null || cashBalance.compareTo(request.getJoinAmount()) < 0){
          throw new IllegalArgumentException("계좌 잔액이 부족합니다.");
        }
    }

    // 우대 조건 충족 여부에 따라 적용 금리 계산
    private BigDecimal calculateAppliedRate(
            ProductSubscriptionInfoDto subscriptionInfo,
            ProductSubscriptionRequestDto request
    ){
        BigDecimal appliedRate;

        if (Boolean.TRUE.equals(request.getPreferentialRateApplied())) {
            appliedRate = subscriptionInfo.getMaximumInterestRate();

            if (appliedRate == null) {
                throw new IllegalArgumentException("우대 금리 정보가 없는 상품입니다.");
            }
        } else {
            appliedRate = subscriptionInfo.getInterestRate();

            if (appliedRate == null) {
                throw new IllegalArgumentException("기본 금리 정보가 없는 상품입니다.");
            }
        }

        return appliedRate;
    }

    // 보유 상품 저장 정보를 생성
    private ProductHoldingCreateDto createHoldingProduct(
            ProductSubscriptionInfoDto subscriptionInfo,
            ProductSubscriptionRequestDto request,
            BigDecimal appliedRate,
            LocalDate startDate,
            LocalDate maturityDate
    ) {
        ProductHoldingCreateDto holdingProduct = new ProductHoldingCreateDto();

        holdingProduct.setAccountId(subscriptionInfo.getAccountId());

        holdingProduct.setProductOptionId(subscriptionInfo.getProductOptionId());

        holdingProduct.setJoinAmount(request.getJoinAmount());

        holdingProduct.setAppliedRate(appliedRate);
        holdingProduct.setStartDate(startDate);
        holdingProduct.setMaturityDate(maturityDate);
        holdingProduct.setStatus(ACTIVE);

        if(SAVING.equals(subscriptionInfo.getProductType())) {
            holdingProduct.setTotalInstallments(subscriptionInfo.getSavingTerm());
            holdingProduct.setPaidInstallments(1);
            holdingProduct.setPaymentDate(calculateNextPaymentDate(startDate, request.getPaymentDay()));
        }
        return holdingProduct;
    }

    // 다음 적금 납입일 계산
    private LocalDate calculateNextPaymentDate(LocalDate startDate, Integer paymentDay) {
        return startDate.plusMonths(1).withDayOfMonth(paymentDay);
    }

    // 계좌 잔액 차감
    private void decreaseAccountCashBalance(Long accountId, BigDecimal amount) {
        int updateCount = productHoldingMapper.decreaseAccountCashBalance(accountId, amount);

        if(updateCount != 1){
            throw new IllegalArgumentException("계좌 잔액이 부족합니다.");
        }
    }

    // 보유 예적금 정보 저장
    private void saveHoldingProduct(ProductHoldingCreateDto holdingProduct){
        int saveCount = productHoldingMapper.saveHoldingProduct(holdingProduct);

        if(saveCount != 1 || holdingProduct.getHoldingProductId() == null) {
            throw new IllegalArgumentException("보유 상품 저장에 실패했습니다.");
        }
    }

    // 상품 거래 및 계좌 거래 내역 저장
    private void saveTransactions(Long accountId, Long holdingProductId, BigDecimal amount, Integer installmentNumber) {
        int productTransactionCount = productHoldingMapper.saveProductSubscriptionTransaction(
                holdingProductId,
                amount,
                installmentNumber
        );

        if(productTransactionCount != 1) {
            throw new IllegalArgumentException("상품 거래 내역 저장에 실패했습니다.");
        }

        int accountTransactionCount = productHoldingMapper.saveAccountWithdrawlTransaction(accountId, amount);

        if(accountTransactionCount != 1) {
            throw new IllegalArgumentException("계좌 거래 내역 저장에 실패했습니다.");
        }
    }

    // 가입 결과 응답 생성
    private ProductSubscriptionResponseDto createResponse(ProductSubscriptionInfoDto subscriptionInfo, ProductHoldingCreateDto holdingProduct){
        ProductSubscriptionResponseDto response = new ProductSubscriptionResponseDto();

        response.setHoldingProductId(
                holdingProduct.getHoldingProductId()
        );
        response.setProductOptionId(
                holdingProduct.getProductOptionId()
        );
        response.setProductType(
                subscriptionInfo.getProductType()
        );
        response.setFinancialCompanyName(
                subscriptionInfo.getFinancialCompanyName()
        );
        response.setProductName(
                subscriptionInfo.getProductName()
        );
        response.setJoinAmount(
                holdingProduct.getJoinAmount()
        );
        response.setAppliedRate(
                holdingProduct.getAppliedRate()
        );
        response.setTotalInstallments(
                holdingProduct.getTotalInstallments()
        );
        response.setPaidInstallments(
                holdingProduct.getPaidInstallments()
        );
        response.setStartDate(
                holdingProduct.getStartDate()
        );
        response.setMaturityDate(
                holdingProduct.getMaturityDate()
        );
        response.setNextPaymentDate(
                holdingProduct.getPaymentDate()
        );
        response.setStatus(
                holdingProduct.getStatus()
        );
        return response;
    }
}
