package org.kkobi.product.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PreferentialRateConditionResponseDto {

    // 우대조건 식별자
    private Long preferentialRateConditionId;

    // 우대조건 유형
    private String conditionType;

    // 사용자에게 표시할 우대조건 이름
    private String conditionName;

    // 조건 충족 시 추가되는 금리
    private BigDecimal additionalRate;

    // 사용자가 직접 선택할 수 있는 조건인지 여부
    private Boolean selectable;

    // 우대조건 표시 순서
    private Integer displayOrder;
}
