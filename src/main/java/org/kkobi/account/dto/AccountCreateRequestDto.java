package org.kkobi.account.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountCreateRequestDto {

    // 가상투자를 시작할 때 설정하는 초기 투자금
    private BigDecimal seedMoney;

    // 매월 투자할 금액
    private BigDecimal monthlyInvestAmount;
}
