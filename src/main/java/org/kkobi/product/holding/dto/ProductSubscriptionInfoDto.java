package org.kkobi.product.holding.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSubscriptionInfoDto {

    // 로그인 사용자의 계좌 식별자
    private Long accountId;

    // 계좌에서 사용할 수 있는 현금 잔액
    private BigDecimal cashBalance;

    // 가입할 상품 옵션 식별자
    private Long productOptionId;

    // 상품 유형, DEPOSIT 또는 SAVING
    private String productType;

    // 금융회사명
    private String financialCompanyName;

    // 상품명
    private String productName;

    // 상품의 최고 가입 한도
    private BigDecimal maxLimit;

    // 가입 기간
    private Integer savingTerm;

    // 기본 금리
    private BigDecimal interestRate;

    // 최고 우대 금리
    private BigDecimal maximumInterestRate;

    // 적립 유형, 예금은 NONE
    private String reserveType;
}
