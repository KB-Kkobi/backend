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
    );

    private final String description;
    private final String criteria;

    RuleAccumulationCondition(String description, String criteria) {
        this.description = description;
        this.criteria = criteria;
    }

    public int getApplicationLimit(BehaviorRuleCode ruleCode) {
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
}
