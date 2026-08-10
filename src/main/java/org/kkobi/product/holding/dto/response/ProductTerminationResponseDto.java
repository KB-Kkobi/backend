package org.kkobi.product.holding.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductTerminationResponseDto {

    // 해지한 보유 상품 식별자
    private Long holdingProductId;

    // 상품 유형
    private String productType;

    // 금융회사명
    private String financialCompanyName;

    // 상품명
    private String productName;

    // 해지 시 반환되는 원금
    private BigDecimal principal;

    // 해지일까지 발생한 세전 이자
    private BigDecimal accruedInterest;

    // 이자소득세
    private BigDecimal interestTax;

    // 세금 차감 후 이자
    private BigDecimal afterTaxInterest;

    // 계좌로 반환되는 최종 금액
    private BigDecimal refundAmount;

    // 해당 연도 누적 이자소득
    private BigDecimal annualInterestIncome;

    // 연간 이자소득 2,000만원 초과 여부
    private Boolean interestIncomeThresholdExceeded;

    // 이자소득 관련 안내
    private String taxNotice;

    // 해지 일시
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy--MM-dd HH:mm:ss",
            timezone = "Asia/Seoul"
    )
    private LocalDateTime terminatedAt;

    // 보유 상태
    private String status;
}
