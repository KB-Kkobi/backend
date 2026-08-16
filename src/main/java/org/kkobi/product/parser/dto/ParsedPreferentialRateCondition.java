package org.kkobi.product.parser.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kkobi.product.enums.PreferentialConditionType;
import org.kkobi.product.enums.PreferentialRateConditionRole;

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

    // 같은 UI 그룹을 묶기 위한 식별자
    private Long conditionGroupId;

    // 우대조건의 UI 역할
    private PreferentialRateConditionRole conditionRole;

    // 기존 독립 우대조건 생성 시 기본 그룹 정보를 활용
    public ParsedPreferentialRateCondition(
            PreferentialConditionType conditionType,
            String conditionName,
            BigDecimal additionalRate,
            boolean selectable
    ){
        this(
                conditionType,
                conditionName,
                additionalRate,
                selectable,
                null,
                PreferentialRateConditionRole.STANDALONE
        );
    }
}
