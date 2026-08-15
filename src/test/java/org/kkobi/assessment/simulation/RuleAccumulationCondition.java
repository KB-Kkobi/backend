package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.enums.BehaviorRuleCode;

@Getter
public enum RuleAccumulationCondition {

    UNLIMITED("기존 무제한 누적", "동일 규칙을 횟수 제한 없이 반영"),
    MEDIUM_P95_HARD_CAP("중간 빈도 P95 상한", "P95 초과 규칙은 점수에 반영하지 않음"),
    MEDIUM_P95_HALF_ATTENUATION(
            "중간 빈도 P95 이후 50% 감쇠",
            "P95 초과 규칙은 기존 점수의 50%만 반영"
    ),
    BULL_BUY_MEDIAN_HALF_ATTENUATION(
            "급등 매수 중앙값 이후 50% 감쇠",
            "급등장 매수는 사용자당 중앙값 2회 이후 점수를 50%만 반영"
    ),
    BULL_BUY_MEDIAN_HARD_CAP(
            "급등 매수 중앙값 상한",
            "급등장 매수는 사용자당 중앙값인 최대 2회까지만 반영"
    ),
    DOMINANT_BUY_RULE_HARD_CAP(
            "반복 기여가 큰 매수 규칙 상한",
            "급락 매수 4회, 급등 매수 2회, 물타기 2회까지만 반영"
    );

    private final String description;
    private final String criteria;

    RuleAccumulationCondition(String description, String criteria) {
        this.description = description;
        this.criteria = criteria;
    }

    public int getApplicationLimit(BehaviorRuleCode ruleCode) {
        if (this == BULL_BUY_MEDIAN_HALF_ATTENUATION
                || this == BULL_BUY_MEDIAN_HARD_CAP) {
            return ruleCode == BehaviorRuleCode.BULL_BUY
                    ? 2
                    : Integer.MAX_VALUE;
        }
        if (this == DOMINANT_BUY_RULE_HARD_CAP) {
            return switch (ruleCode) {
                case CRASH_BUY -> 4;
                case BULL_BUY, LOSS_AVERAGING_BUY -> 2;
                default -> Integer.MAX_VALUE;
            };
        }
        return switch (ruleCode) {
            case INITIAL_STOCK_ALLOCATION,
                    INITIAL_DEPOSIT_ALLOCATION,
                    INITIAL_CASH_ALLOCATION,
                    CRASH_FULL_SELL,
                    DEPOSIT_CANCEL_AND_SECURITY_BUY -> 1;
            case CRASH_BUY,
                    BULL_BUY,
                    BULL_PROFIT_SELL,
                    LOSS_CUT_SELL -> 4;
            case LOSS_AVERAGING_BUY -> 2;
            default -> Integer.MAX_VALUE;
        };
    }

    public boolean isHalfAttenuation() {
        return this == MEDIUM_P95_HALF_ATTENUATION
                || this == BULL_BUY_MEDIAN_HALF_ATTENUATION;
    }
}
