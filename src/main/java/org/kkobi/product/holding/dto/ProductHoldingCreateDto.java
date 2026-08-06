package org.kkobi.product.holding.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductHoldingCreateDto {

    // 저장 후 생성되는 보유 상품 식별자
    private Long holdingProductId;

    // 가입한 사용자의 계좌 식별자
    private Long accountId;

    // 가입한 상품 옵션 식별자
    private Long productOptionId;

    // 예금 가입 금액 또는 적금 월 납입 금액
    private BigDecimal joinAmount;

    // 실제 적용되는 금리
    private BigDecimal appliedRate;

    // 전체 납입 횟수, 예금은 null
    private Integer totalIstallments;

    // 완료한 납입 횟수, 예금은 null
    private Integer paidInstallments;

    // 다음 적금 납입일, 예금은 null
    private LocalDate paymentate;

    // 가입일
    private LocalDate startDate;

    // 만기일
    private LocalDate maturityDate;

    // 보유 상태
    private String status;

}
