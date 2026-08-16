package org.kkobi.account.service;


import lombok.RequiredArgsConstructor;
import org.kkobi.account.dto.AccountAssetInfoDto;
import org.kkobi.account.dto.AccountAssetStatusResponseDto;
import org.kkobi.account.mapper.AccountMapper;
import org.kkobi.assessment.service.GameAssessmentService;
import org.kkobi.product.holding.dto.response.SavingsAssetStatusResponseDto;
import org.kkobi.product.holding.service.ProductHoldingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final BigDecimal INITIAL_SEED_MONEY = BigDecimal.valueOf(5_000_000L);

    private final AccountMapper accountMapper;
    private final ProductHoldingService productHoldingService;
    private final GameAssessmentService gameAssessmentService;

    // 가상투자 최초 시작 시 사용자 계좌 생성
    @Transactional
    public void createAccount(Long userId) {
        validateUserId(userId);
        validateCompletedGame(userId);
        validateAccountNotExists(userId);

        int savedCount = accountMapper.saveAccount(userId, INITIAL_SEED_MONEY);

        if (savedCount != 1){
            throw new IllegalStateException("계좌 생성에 실패했습니다.");
        }
    }

    // 성향 파악 게임 완료 여부 확인
    private void validateCompletedGame(Long userId) {
        if(!gameAssessmentService.existsCompletedGame(userId)){
            throw new IllegalArgumentException(
                    "성향 파악 게임을 완료한 후 계좌를 생성할 수 있습니다."
            );
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

    // 사용자의 기존 계좌가 존재하는지 확인
    private void validateAccountNotExists(Long userId){
        if(accountMapper.existsAccountByUserId(userId)){
            throw new IllegalStateException("이미 생성된 계좌가 존재합니다.");
        }
    }
}
