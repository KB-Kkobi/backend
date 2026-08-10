package org.kkobi.product.holding.service;


import lombok.RequiredArgsConstructor;
import org.kkobi.product.holding.dto.ProductHoldingCreateDto;
import org.kkobi.product.holding.dto.ProductSubscriptionInfoDto;
import org.kkobi.product.holding.dto.request.ProductSubscriptionRequestDto;
import org.kkobi.product.holding.dto.response.ProductSubscriptionResponseDto;
import org.kkobi.product.holding.dto.ProductHoldingInfoDto;
import org.kkobi.product.holding.dto.response.ProductHoldingListItemResponseDto;
import org.kkobi.product.holding.dto.response.ProductTerminationResponseDto;
import org.kkobi.product.holding.dto.response.SavingsAssetStatusResponseDto;
import org.kkobi.product.mapper.ProductHoldingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductHoldingService {

    private static final String DEPOSIT = "DEPOSIT";
    private static final String SAVING = "SAVING";
    private static final String ACTIVE = "ACTIVE";

    private static final String TERMINATED = "TERMINATED";

    // 금융소득 안내 기준
    private static final BigDecimal INTEREST_INCOME_THRESHOLD =
            new BigDecimal("20000000");

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private final ProductHoldingMapper productHoldingMapper;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");

    // 이자 소득세율
    private static final BigDecimal INTEREST_TAX_RATE =
            new BigDecimal("0.154");

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

    // 로그인 사용자의 보유 예적금을 해지
    @Transactional
    public ProductTerminationResponseDto terminateProduct(
            Long userId,
            Long holdingProductId
    ){
        validateUserId(userId);
        validateHoldingProductId(holdingProductId);

        ProductHoldingInfoDto holdingProduct =
                productHoldingMapper.getHoldingProductForTermination(
                        userId,
                        holdingProductId
                );

        LocalDateTime terminatedAt =
                LocalDateTime.now(KOREA_ZONE_ID);

        LocalDate terminationDate =
                terminatedAt.toLocalDate();

        validateTerminationProduct(holdingProduct, terminationDate);

        // 현재까지 납입한 원금 계산
        BigDecimal principal =
                calculateCurrentPrincipal(holdingProduct);

        // 해지일까지 발생한 이자 계산
        BigDecimal accruedInterest =
                calculateAccruedInterest(
                        holdingProduct,
                        terminationDate
                );

        BigDecimal interestTax =
                calculateInterestTax(accruedInterest);

        BigDecimal afterTaxInterest =
                accruedInterest.subtract(interestTax);

        BigDecimal refundAmount =
                principal.add(afterTaxInterest);

        terminateHoldingProduct(
                holdingProductId
        );

        increaseAccountCashBalance(
                holdingProduct.getAccountId(),
                refundAmount
        );

        saveTerminationTransactions(
                holdingProduct.getAccountId(),
                holdingProductId,
                refundAmount,
                accruedInterest,
                interestTax,
                terminatedAt
        );

        LocalDateTime yearStart =
                terminationDate
                        .withDayOfYear(1)
                        .atStartOfDay();

        LocalDateTime nextYearStart =
                yearStart.plusYears(1);

        BigDecimal annualInterestIncome =
                productHoldingMapper.getAnnualInterestIncome(
                        userId,
                        yearStart,
                        nextYearStart
                );

        boolean thresholdExceeded =
                annualInterestIncome.compareTo(
                        INTEREST_INCOME_THRESHOLD
                ) > 0;

        return createTerminationResponse(
                holdingProduct,
                principal,
                accruedInterest,
                interestTax,
                afterTaxInterest,
                refundAmount,
                annualInterestIncome,
                thresholdExceeded,
                terminatedAt
        );
    }

    // 로그인 사용자의 보유 예적금 목록 조회
    @Transactional(readOnly = true)
    public List<ProductHoldingListItemResponseDto> getHoldingProducts(Long userId){
        validateUserId(userId);

        List<ProductHoldingInfoDto> holdingProducts = productHoldingMapper.getHoldingProductsByUserId(userId);

        return holdingProducts.stream()
                .map(this::createHoldingListItemResponse)
                .toList();
    }

    // 로그인 사용자의 전체 저축 자산 현황 조회
    @Transactional(readOnly = true)
    public SavingsAssetStatusResponseDto getSavingAssetStatus(Long userId) {
        validateUserId(userId);

        List<ProductHoldingListItemResponseDto> holdings =
                getHoldingProducts(userId);

        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalAccuredInterest = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal totalAfterTaxInterest = BigDecimal.ZERO;
        BigDecimal totalAfterTaxCurrentValue = BigDecimal.ZERO;

        for(ProductHoldingListItemResponseDto holding : holdings) {
            totalPrincipal = totalPrincipal.add(holding.getCurrentPrincipal());

            totalAccuredInterest = totalAccuredInterest.add(holding.getAccruedInterest());

            totalCurrentValue = totalCurrentValue.add(holding.getCurrentValue());

            totalAfterTaxInterest = totalAfterTaxInterest.add(holding.getAfterTaxInterest());

            totalAfterTaxCurrentValue = totalAfterTaxCurrentValue.add(holding.getAfterTaxCurrentValue());
        }

        BigDecimal totalReturnRate = calculateReturnRate(totalPrincipal, totalAfterTaxCurrentValue);

        SavingsAssetStatusResponseDto response = new SavingsAssetStatusResponseDto();

        response.setTotalPrincipal(totalPrincipal);
        response.setTotalAccruedInterest(totalAccuredInterest);
        response.setTotalCurrentValue(totalCurrentValue);
        response.setTotalAfterTaxInterest(totalAfterTaxInterest);
        response.setTotalAfterTaxCurrentValue(totalAfterTaxCurrentValue);
        response.setTotalReturnRate(totalReturnRate);
        response.setHoldings(holdings);

        return response;
    }

    // 보유 예적금 목록 응답 생성
    private ProductHoldingListItemResponseDto createHoldingListItemResponse(ProductHoldingInfoDto holdingProduct){
        LocalDate today = LocalDate.now(KOREA_ZONE_ID);

        BigDecimal currentPrincipal =  calculateCurrentPrincipal(holdingProduct);

        BigDecimal accruedInterest = calculateAccruedInterest(holdingProduct, today);

        BigDecimal currentValue = currentPrincipal.add(accruedInterest);

        BigDecimal afterTaxInterest = calculateAfterTaxInterest(accruedInterest);

        BigDecimal afterTaxCurrentValue = currentPrincipal.add(afterTaxInterest);

        BigDecimal returnRate = calculateReturnRate(currentPrincipal, afterTaxCurrentValue);

        BigDecimal expectedInterest = calculateExpectedInterest(holdingProduct);

        BigDecimal expectedPrincipal = calculateExpectedPrincipal(holdingProduct);

        ProductHoldingListItemResponseDto response = new ProductHoldingListItemResponseDto();

        response.setHoldingProductId(
                holdingProduct.getHoldingProductId()
        );
        response.setProductOptionId(
                holdingProduct.getProductOptionId()
        );
        response.setProductType(
                holdingProduct.getProductType()
        );
        response.setFinancialCompanyName(
                holdingProduct.getFinancialCompanyName()
        );
        response.setProductName(
                holdingProduct.getProductName()
        );
        response.setReserveTypeName(
                holdingProduct.getReserveTypeName()
        );
        response.setJoinAmount(
                holdingProduct.getJoinAmount()
        );
        response.setAppliedRate(
                holdingProduct.getAppliedRate()
        );
        response.setSavingTerm(
                holdingProduct.getSavingTerm()
        );
        response.setTotalInstallments(
                holdingProduct.getTotalInstallments()
        );
        response.setPaidInstallments(
                holdingProduct.getPaidInstallments()
        );

        response.setCurrentPrincipal(currentPrincipal);

        response.setAccruedInterest(accruedInterest);

        response.setCurrentValue(currentValue);

        response.setAfterTaxInterest(afterTaxInterest);

        response.setAfterTaxCurrentValue(afterTaxCurrentValue);

        response.setReturnRate(returnRate);

        response.setExpectedInterest(expectedInterest);
        response.setExpectedMaturityAmount(
                expectedPrincipal.add(expectedInterest)
        );

        response.setMaturityProgressRate(
                calculateMaturityProgressRate(
                        holdingProduct.getStartDate(),
                        holdingProduct.getMaturityDate(),
                        today
                )
        );

        response.setRemainingDays(
                calculateRemainingDays(
                        holdingProduct.getMaturityDate(),
                        today
                )
        );

        response.setStartDate(
                holdingProduct.getStartDate()
        );
        response.setMaturityDate(
                holdingProduct.getMaturityDate()
        );
        response.setNextPaymentDate(
                holdingProduct.getNextPaymentDate()
        );
        response.setStatus(
                holdingProduct.getStatus()
        );

        return response;
    }

    // 현재까지 실제 납입한 원금 계산
    private BigDecimal calculateCurrentPrincipal(ProductHoldingInfoDto holdingProduct){
        if(DEPOSIT.equals(holdingProduct.getProductType())){
            return holdingProduct.getJoinAmount();
        }

        int paidInstallments = holdingProduct.getPaidInstallments() == null ? 0 : holdingProduct.getPaidInstallments();

        return holdingProduct.getJoinAmount().multiply(BigDecimal.valueOf(paidInstallments));
    }

    // 만기까지 정상 납입했을 때 전체 원금 계산
    private BigDecimal calculateExpectedPrincipal(ProductHoldingInfoDto holdingProduct){
        if(DEPOSIT.equals(holdingProduct.getProductType())){
            return holdingProduct.getJoinAmount();
        }

        int totalInstallments = holdingProduct.getTotalInstallments() == null ? 0 : holdingProduct.getTotalInstallments();

        return holdingProduct.getJoinAmount().multiply(BigDecimal.valueOf(totalInstallments));
    }

    // 현재까지 발생한 예상 이자 계산
    private BigDecimal calculateAccruedInterest(ProductHoldingInfoDto holdingProduct, LocalDate today){
        LocalDate evaluationDate = today.isAfter(holdingProduct.getMaturityDate()) ? holdingProduct.getMaturityDate() : today;

        if(evaluationDate.isBefore(holdingProduct.getStartDate())) {
            return BigDecimal.ZERO;
        }

        if(DEPOSIT.equals(holdingProduct.getProductType())) {
            long interestDays = ChronoUnit.DAYS.between(
                    holdingProduct.getStartDate(), evaluationDate);

            return calculateSimpleInterest(
                    holdingProduct.getJoinAmount(),
                    holdingProduct.getAppliedRate(),
                    interestDays
            );
        }

        return calculateSavingAccruedInterest(
                holdingProduct,
                evaluationDate
        );
    }

    // 적금의 현재까지 발생한 이자 계산
    private BigDecimal calculateSavingAccruedInterest(ProductHoldingInfoDto holdingProduct, LocalDate evaluationDate) {
        int paidInstallments = holdingProduct.getPaidInstallments() == null ? 0 : holdingProduct.getPaidInstallments();

        BigDecimal totalInterest = BigDecimal.ZERO;

        for(int installment = 0; installment < paidInstallments; installment++){
            LocalDate paymentDate = calculateInstallmentDate(holdingProduct, installment);

            if(paymentDate.isAfter(evaluationDate)){
                continue;
            }

            long interestDays =
                    ChronoUnit.DAYS.between(
                            paymentDate,
                            evaluationDate
                    );

            totalInterest = totalInterest.add(calculateSimpleInterest(
                    holdingProduct.getJoinAmount(),
                    holdingProduct.getAppliedRate(),
                    interestDays));
        }
        return totalInterest;
    }

    // 만기까지 정상 유지했을 때 예상 이자 계산
    private BigDecimal calculateExpectedInterest(ProductHoldingInfoDto holdingProduct){
        if(DEPOSIT.equals(holdingProduct.getProductType())){
            long interestDays =
                    ChronoUnit.DAYS.between(
                            holdingProduct.getStartDate(),
                            holdingProduct.getMaturityDate()
                    );

            return calculateSimpleInterest(
                    holdingProduct.getJoinAmount(),
                    holdingProduct.getAppliedRate(),
                    interestDays
            );
        }

        int totalInstallments =
                holdingProduct.getTotalInstallments() == null ? 0 : holdingProduct.getTotalInstallments();

        BigDecimal totalInterest = BigDecimal.ZERO;

        for(int installment = 0; installment < totalInstallments; installment ++){
            LocalDate paymentDate =
                    calculateInstallmentDate(
                            holdingProduct, installment
                    );

            long interestDays =
                    ChronoUnit.DAYS.between(
                            paymentDate,
                            holdingProduct.getMaturityDate()
                    );

            if(interestDays <= 0){
                continue;
            }

            totalInterest = totalInterest.add(
                    calculateSimpleInterest(
                            holdingProduct.getJoinAmount(),
                            holdingProduct.getAppliedRate(),
                            interestDays
                    )
            );
        }
        return totalInterest;
    }

    // 적금 회차별 납입일 계산
    private LocalDate calculateInstallmentDate(
            ProductHoldingInfoDto holdingProduct,
            int installmentIndex
    ) {
        if (installmentIndex == 0) {
            return holdingProduct.getStartDate();
        }

        int paymentDay =
                holdingProduct.getNextPaymentDate() == null ? holdingProduct.getStartDate().getDayOfMonth() :
                        holdingProduct.getNextPaymentDate().getDayOfMonth();

        return holdingProduct.getStartDate().plusMonths(installmentIndex).withDayOfMonth(paymentDay);
    }

    // 원금, 연이율, 보유 일수를 기준으로 단리 계산
    private BigDecimal calculateSimpleInterest(
            BigDecimal principal, BigDecimal annualRate, long interestDays
    ) {
        if(principal == null || annualRate == null || interestDays <= 0){
            return BigDecimal.ZERO;
        }

        return principal.multiply(annualRate)
                .multiply(BigDecimal.valueOf(interestDays))
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(DAYS_PER_YEAR, 0, RoundingMode.DOWN);
    }

    // 이자소득세를 반영한 후 세후 이자 계산
    private BigDecimal calculateAfterTaxInterest(
            BigDecimal accruedInterest
    ) {
        if (accruedInterest == null
                || accruedInterest.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return accruedInterest.subtract(
                calculateInterestTax(accruedInterest)
        );
    }

    // 현재 원금 대비 세후 평가금액의 수익률 계산
    private BigDecimal calculateReturnRate(BigDecimal currentPrincipal, BigDecimal afterTaxCurrentValue){
        if(currentPrincipal == null || currentPrincipal.compareTo(BigDecimal.ZERO) <= 0){
            return new BigDecimal("0.00");
        }

        return afterTaxCurrentValue
                .subtract(currentPrincipal)
                .multiply(ONE_HUNDRED)
                .divide(currentPrincipal, 2, RoundingMode.HALF_UP);
    }

    // 가입일부터 만기일까지의 진행률 계산
    private BigDecimal calculateMaturityProgressRate(
            LocalDate startDate,
            LocalDate maturityDate,
            LocalDate today
    ) {
        long totalDays =
                ChronoUnit.DAYS.between(
                        startDate, maturityDate
                );

        if(totalDays <= 0) {
            return new BigDecimal("100.00");
        }

        if(!today.isAfter(startDate)) {
            return new BigDecimal("0.00");
        }

        if(!today.isBefore(maturityDate)) {
            return new BigDecimal("100.00");
        }

        long elapsedDays = ChronoUnit.DAYS.between(startDate, today);

        return BigDecimal.valueOf(elapsedDays)
                .multiply(ONE_HUNDRED)
                .divide(
                        BigDecimal.valueOf(totalDays),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    // 만기일까지 남은 일수 계산
    private Long calculateRemainingDays(
            LocalDate maturityDate,
            LocalDate today
    ) {
        if(!today.isBefore(maturityDate)){
            return 0L;
        }

        return ChronoUnit.DAYS.between(
                today,
                maturityDate
        );
    }

    // 로그인 사용자 정보 확인
    private void validateUserId(Long userId) {
        if(userId == null){
            throw new IllegalArgumentException("사용자 정보가 없습니다.");
        }
    }

    // 발생한 이자에 대한 이자소득세 계산
    private BigDecimal calculateInterestTax(
            BigDecimal accruedInterest
    ){
        if(accruedInterest == null
        || accruedInterest.compareTo(BigDecimal.ZERO)<= 0){
            return BigDecimal.ZERO;
        }

        return accruedInterest
                .multiply(INTEREST_TAX_RATE)
                .setScale(0, RoundingMode.DOWN);
    }

    // 보유 상품 식별자 확인
    private void validateHoldingProductId(Long holdingProductId) {
        if(holdingProductId == null || holdingProductId <= 0){
            throw new IllegalArgumentException(
                    "보유 상품 ID가 올바르지 않습니다."
            );
        }
    }

    // 예적금 해지 가능 여부 확인
    private void validateTerminationProduct(
            ProductHoldingInfoDto holdingProduct,
            LocalDate terminationDate
    ) {
        if(holdingProduct == null){
            throw new IllegalArgumentException(
                    "해지할 보유 상품을 찾을 수 없습니다."
            );
        }

        if(!ACTIVE.equals(holdingProduct.getStatus())){
            throw new IllegalArgumentException(
                    "이미 해지되었거나 해지할 수 없는 상품입니다."
            );
        }

        if(holdingProduct.getAccountId() == null){
            throw new IllegalArgumentException(
                    "연결된 계좌 정보를 찾을 수 없습니다."
            );
        }

        if(holdingProduct.getMaturityDate() == null){
            throw new IllegalArgumentException(
                    "상품 만기 정보를 찾을 수 없습니다."
            );
        }

        if(!terminationDate.isBefore(holdingProduct.getMaturityDate())) {
            throw new IllegalArgumentException(
                    "만기된 상품은 해지할 수 없습니다."
            );
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

    // 보유 예적금 상태를 해지로 변경
    private void terminateHoldingProduct(Long holdingProductId) {
        int updateCount = productHoldingMapper.terminateHoldingProduct(
                holdingProductId
        );

        if(updateCount != 1){
            throw new IllegalArgumentException(
                    "예적금 해지 처리에 실패했습니다."
            );
        }
    }

    // 해지 반환 금액을 계좌에 입금
    private void increaseAccountCashBalance(Long accountId, BigDecimal amount){
        int updateCount =
                productHoldingMapper.increaseAccountCashBalance(
                        accountId,
                        amount
                );

        if(updateCount != 1){
            throw new IllegalArgumentException(
                    "해지 금액 반환에 실패했습니다."
            );
        }
    }

    // 상품 해지 및 계좌 입금 거래 내역 저장
    private void saveTerminationTransactions(
            Long accountId,
            Long holdingProductId,
            BigDecimal refundAmount,
            BigDecimal interestAmount,
            BigDecimal interestTaxAmount,
            LocalDateTime terminatedAt
    ) {
        int productTransactionCount =
                productHoldingMapper.saveProductTerminationTransaction(
                        holdingProductId,
                        refundAmount,
                        interestAmount,
                        interestTaxAmount,
                        terminatedAt
                );

        if(productTransactionCount != 1){
            throw new IllegalArgumentException(
                    "상품 해지 거래 내역 저장에 실패했습니다."
            );
        }

        int accountTransactionCount =
                productHoldingMapper.saveAccountDepositTransaction(
                        accountId,
                        refundAmount
                );

        if(accountTransactionCount != 1){
            throw new IllegalArgumentException(
                    "계좌 입금 거래 내역 저장에 실패했습니다."
            );
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

        int accountTransactionCount = productHoldingMapper.saveAccountWithdrawalTransaction(accountId, amount);

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

    // 예적금 해지 결과 응답 생성
    private ProductTerminationResponseDto createTerminationResponse(
            ProductHoldingInfoDto holdingProduct,
            BigDecimal principal,
            BigDecimal accruedInterest,
            BigDecimal interestTax,
            BigDecimal afterTaxInterest,
            BigDecimal refundAmount,
            BigDecimal annualInterestIncome,
            boolean thresholdExceeded,
            LocalDateTime terminatedAt
    ) {
        ProductTerminationResponseDto response =
                new ProductTerminationResponseDto();

        response.setHoldingProductId(
                holdingProduct.getHoldingProductId()
        );
        response.setProductType(
                holdingProduct.getProductType()
        );
        response.setFinancialCompanyName(
                holdingProduct.getFinancialCompanyName()
        );
        response.setProductName(
                holdingProduct.getProductName()
        );

        response.setPrincipal(principal);
        response.setAccruedInterest(accruedInterest);
        response.setInterestTax(interestTax);
        response.setAfterTaxInterest(afterTaxInterest);
        response.setRefundAmount(refundAmount);

        response.setAnnualInterestIncome(
                annualInterestIncome
        );
        response.setInterestIncomeThresholdExceeded(
                thresholdExceeded
        );

        if (thresholdExceeded) {
            response.setTaxNotice(
                    "연간 이자소득이 2,000만 원을 초과했습니다."
            );
        }

        response.setTerminatedAt(
                terminatedAt
        );
        response.setStatus(
                TERMINATED
        );

        return response;
    }
}
