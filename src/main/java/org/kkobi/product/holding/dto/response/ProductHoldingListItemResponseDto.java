package org.kkobi.product.holding.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductHoldingListItemResponseDto {

    // 보유 상품 식별자
    private Long holdingProductId;

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

    // 현재까지 납입한 원금
    private BigDecimal currentPrincipal;

    // 현재까지 발생한 예상 이자
    private BigDecimal accruedInterest;

    // 현재 평가금액
    private BigDecimal currentValue;

    // 만기 예상 이자
    private BigDecimal expectedInterest;

    // 만기 예상 금액
    private BigDecimal expectedMaturityAmount;

    // 만기 진행률
    private BigDecimal maturityProgressRate;

    // 만기까지 남은 일수
    private Long remainingDays;

    // 가입일
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    // 만기일
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maturityDate;

    // 다음 납입일
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextPaymentDate;

    // 보유 상태
    private String status;

}
