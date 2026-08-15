package org.kkobi.product.parser.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kkobi.product.enums.PreferentialConditionType;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ParsedPreferentialRateCondition {

    // 우대조건 유형
    private PreferentialConditionType conditionType;

    // 사용자에게 표시할 우대조건 내용
    private String conditionName;

    // 조건 충족 시 추가되는 우대금리
    private BigDecimal additionalRate;

    // 사용자가 직접 선택할 수 있는 조건인지 여부
    private boolean selectable;
}
