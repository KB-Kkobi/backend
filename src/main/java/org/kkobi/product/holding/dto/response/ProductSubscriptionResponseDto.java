package org.kkobi.product.holding.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductSubscriptionResponseDto {

    // 생성된 보유 예적금 식별자
    private Long holdingProductId;

    // 가입한 상품 옵션 식별자
    private Long productOptionId;

    // 상품 유형, DEPOSIT 또는 SAVING
    private String productType;

    // 금융회사명
    private String financialCompanyName;

    // 상품명
    private String productName;

    // 예금 가입 금액 또는 적금 월 납입 금액
    private BigDecimal joinAmount;

    // 실제 적용 금리
    private BigDecimal appliedRate;

    // 전체 납입 횟수, 예금은 null
    private Integer totalInstallments;

    // 완료된 납입 횟수, 예금은 null
    private Integer paidInstallments;

    // 가입일
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd",
            timezone = "Asia/Seoul"
    )
    private LocalDate startDate;

    // 만기일
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd",
            timezone = "Asia/Seoul"
    )
    private LocalDate maturityDate;

    // 다음 납입일, 예금은 null
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd",
            timezone = "Asia/Seoul"
    )
    private LocalDate nextPaymentDate;

    // 보유 상태
    private String status;
}
