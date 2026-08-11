package org.kkobi.product.holding.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductHoldingInfoDto {

    // 보유 상품 식별자
    private Long holdingProductId;

    // 계좌 식별자
    private Long accountId;

    // 상품 옵션 식별자
    private Long productOptionId;

    // 상품 유형
    private String productType;

    // 금융회사명
    private String financialCompanyName;

    // 상품명
    private String productName;

    // 적립 유형명
    private String reserveTypeName;

    // 예금 가입 금액 또는 적금 월 납입 금액
    private BigDecimal joinAmount;

    // 적용 금리
    private BigDecimal appliedRate;

    // 가입 기간
    private Integer savingTerm;

    // 전체 납입 횟수
    private Integer totalInstallments;

    // 완료한 납입 횟수
    private Integer paidInstallments;

    // 다음 납입일
    private LocalDate nextPaymentDate;

    // 가입일
    private LocalDate startDate;

    // 만기일
    private LocalDate maturityDate;

    // 보유 상태
    private String status;
}
