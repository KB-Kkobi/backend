package org.kkobi.product.holding.dto.request;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductSubscriptionRequestDto {

    // 가입할 상품 금리 옵션 식별자
    @NotNull(message = "상품 옵션 ID는 필수입니다")
    @Positive(message = "상품 옵션 ID는 1 이상이어야 합니다.")
    private Long productOptionId;

    // 예금 가입 금액 또는 적금 월 납입 금액
    @NotNull(message = "가입 금액은 필수입니다.")
    @DecimalMin(
            value = "1",
            message = "가입 금액은 1원 이상이어야 합니다."
    )
    private BigDecimal joinAmount;

    // 사용자가 선택한 우대조건 식별자 목록
    private List<Long> selectedPreferentialRateConditionIds = new ArrayList<>();

    // 적금 월 납입일, 예금은 사용하지 않음
    @Min(
            value = 1,
            message = "납입일은 1일 이상이어야 합니다."
    )
    @Max(
            value = 28,
            message = "납입일은 28일 이하여야 합니다."
    )
    private Integer paymentDay;
}
