package org.kkobi.product.enums;

public enum PreferentialRateConditionRole {

    // 독립적으로 표시 되는 우대조건
    STANDALONE,

    // 그룹 전체에 적용되는 안내 또는 전제조건
    GROUP_NOTICE,

    // 그룹 내부의 세부 설명
    GROUP_DETAIL,

    // 실제 금리가 적용되거나 선택 가능한 조건
    GROUP_CONDITION
}
