package org.kkobi.account.service;


import lombok.RequiredArgsConstructor;
import org.kkobi.account.dto.AccountCreateRequestDto;
import org.kkobi.account.mapper.AccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
