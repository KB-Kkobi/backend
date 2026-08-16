package org.kkobi.product.holding.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PreferentialRateConditionInfoDto {

    // 우대조건 식별자
    private Long preferentialRateConditionId;

    // 우대조건이 속한 상품 옵션 식별자
    private Long productionOptionId;

    // 조건 충족 시 추가되는 우대금리
    private BigDecimal additionalRate;

    // 사용자가 직접 선택할 수 있는 조건인지 여부
    private Boolean selectable;
}
