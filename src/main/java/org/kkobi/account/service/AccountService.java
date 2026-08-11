package org.kkobi.account.service;


import lombok.RequiredArgsConstructor;
import org.kkobi.account.dto.AccountAssetInfoDto;
import org.kkobi.account.dto.AccountAssetStatusResponseDto;
import org.kkobi.account.dto.AccountCreateRequestDto;
import org.kkobi.account.mapper.AccountMapper;
import org.kkobi.product.holding.dto.response.SavingsAssetStatusResponseDto;
import org.kkobi.product.holding.service.ProductHoldingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AccountService {

    // 초기 투자금 최대 10억원
    private static final BigDecimal MAXIMUM_SEED_MONEY =
            BigDecimal.valueOf(1_000_000_000L);

    // 월 투자금 최대 1,000만원
    private static final BigDecimal MAXIMUM_MONTHLY_INVEST_AMOUNT =
            BigDecimal.valueOf(10_000_000L);

    private final AccountMapper accountMapper;
    private final ProductHoldingService productHoldingService;

    // 가상투자 최초 시작 시 사용자 계좌 생성
    @Transactional
    public void createAccount(
            Long userId,
            AccountCreateRequestDto request
    ) {
        validateUserId(userId);
        validateRequest(request);
        validateAccountNotExists(userId);
        validateInvestmentAmount(request);

        int savedCount = accountMapper.saveAccount(userId, request);

        if (savedCount != 1){
            throw new IllegalStateException("계좌 생성에 실패했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public AccountAssetStatusResponseDto getAccountAssetStatus(Long userId) {
        validateUserId(userId);

        AccountAssetInfoDto account =
                accountMapper.getAccountAssetInfoByUserId(userId);

        if (account == null) {
            throw new IllegalArgumentException("가상투자 계좌를 찾을 수 없습니다.");
        }

        SavingsAssetStatusResponseDto savings =
                productHoldingService.getSavingAssetStatus(userId);

        BigDecimal cashBalance = defaultZero(account.getCashBalance());
        BigDecimal stockAsset = defaultZero(account.getStockAsset());
        BigDecimal savingsAsset =
                defaultZero(savings.getTotalAfterTaxCurrentValue());
        BigDecimal totalAsset = cashBalance.add(stockAsset).add(savingsAsset);
        BigDecimal seedMoney = defaultZero(account.getSeedMoney());
        BigDecimal totalProfit = totalAsset.subtract(seedMoney);

        AccountAssetStatusResponseDto response =
                new AccountAssetStatusResponseDto();
        response.setAccountId(account.getAccountId());
        response.setSeedMoney(seedMoney);
        response.setMonthlyInvestAmount(
                defaultZero(account.getMonthlyInvestAmount())
        );
        response.setCashBalance(cashBalance);
        response.setStockAsset(stockAsset);
        response.setSavingsAsset(savingsAsset);
        response.setTotalAsset(totalAsset);
        response.setTotalProfit(totalProfit);
        response.setTotalReturnRate(calculateRatio(totalProfit, seedMoney));
        response.setCashRatio(calculateRatio(cashBalance, totalAsset));
        response.setStockRatio(calculateRatio(stockAsset, totalAsset));
        response.setSavingsRatio(calculateRatio(savingsAsset, totalAsset));
        return response;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal calculateRatio(BigDecimal amount, BigDecimal total) {
        if (amount == null || total == null
                || total.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.00");
        }

        return amount
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    // 로그인 사용자 ID가 존재하는지 확인
    private void validateUserId(Long userId){
        if (userId == null){
            throw new IllegalStateException("사용자 정보가 없습니다.");
        }
    }

    // 계좌 생성 요청이 존재하는지 확인
    private void validateRequest(AccountCreateRequestDto request){
        if(request == null){
            throw new IllegalArgumentException("계좌 생성 요청 정보가 없습니다.");
        }

        if(request.getSeedMoney() == null){
            throw new IllegalArgumentException("초기 투자금은 필수입니다.");
        }

        if(request.getMonthlyInvestAmount() == null){
            throw new IllegalArgumentException("월 투자금은 필수ㅡ입니다.");
        }
    }

    // 사용자의 기존 계좌가 존재하는지 확인
    private void validateAccountNotExists(Long userId){
        if(accountMapper.existsAccountByUserId(userId)){
            throw new IllegalStateException("이미 생성된 계좌가 존재합니다.");
        }
    }

    // 초기 투자금과 월 투자금의 허용 범위 확인
    private void validateInvestmentAmount(
            AccountCreateRequestDto request
    ) {
        BigDecimal seedMoney = request.getSeedMoney();
        BigDecimal monthlyInvestAmount =
                request.getMonthlyInvestAmount();

        if(seedMoney.compareTo(BigDecimal.ZERO) < 0
                || seedMoney.compareTo(MAXIMUM_SEED_MONEY) > 0){
            throw new IllegalArgumentException(
                    "초기 투자금은 0원 이상 10억원 이하로 설정해야 합니다."
            );
        }

        if(monthlyInvestAmount.compareTo(BigDecimal.ZERO) < 0
                || monthlyInvestAmount.compareTo(MAXIMUM_MONTHLY_INVEST_AMOUNT) > 0){
            throw new IllegalArgumentException(
                    "월 투자금은 0원 이상 1,000만원 이하로 설정해야 합니다."
            );
        }
    }
}
