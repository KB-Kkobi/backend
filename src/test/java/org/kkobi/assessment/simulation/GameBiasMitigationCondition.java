package org.kkobi.assessment.simulation;

import lombok.Getter;

@Getter
public enum GameBiasMitigationCondition {

    SYMMETRIC_BASELINE(
            "대칭 수량 중간안",
            SameTickRuleApplicationCondition.REPEATED,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
    ),
    ONCE_PER_TICK(
            "동일 Tick 규칙 1회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_FIXED_ACTION_SCORE_AND_DEPOSIT_DECISION
    ),
    RP_CENTERED_BULL_BUY(
            "급등 매수 RP 중심",
            SameTickRuleApplicationCondition.REPEATED,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION
    ),
    RP_CENTERED_AND_ONCE_PER_TICK(
            "급등 매수 RP 중심·동일 Tick 1회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION
    ),
    RP_CENTERED_ONCE_AND_ATTENUATED(
            "급등 매수 RP 중심·동일 Tick 1회·P95 이후 감쇠",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.MEDIUM_P95_HALF_ATTENUATION,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION
    ),
    RP_CENTERED_ONCE_AND_BULL_BUY_ATTENUATED(
            "급등 매수 RP 중심·동일 Tick 1회·급등 매수 2회 이후 감쇠",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HALF_ATTENUATION,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION
    ),
    RP_CENTERED_ONCE_AND_BULL_BUY_CAPPED(
            "급등 매수 RP 중심·동일 Tick 1회·급등 매수 최대 2회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_RP_CENTERED_BULL_BUY_AND_DEPOSIT_DECISION
    ),
    REDUCED_RP_BULL_BUY_AND_ONCE_PER_TICK(
            "급등 매수 RP 5점 제한·동일 Tick 1회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_REDUCED_BULL_BUY_RP_AND_DEPOSIT_DECISION
    ),
    REDUCED_RP_BULL_BUY_ONCE_AND_CAPPED(
            "급등 매수 RP 5점 제한·동일 Tick 1회·급등 매수 최대 2회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_REDUCED_BULL_BUY_RP_AND_DEPOSIT_DECISION
    ),
    SIZE_SEPARATED_BULL_BUY_AND_ONCE_PER_TICK(
            "급등 매수 규모별 RT/RP 분리·동일 Tick 1회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_AND_DEPOSIT_DECISION
    ),
    SIZE_SEPARATED_BULL_BUY_ONCE_AND_CAPPED(
            "급등 매수 규모별 RT/RP 분리·동일 Tick 1회·급등 매수 최대 2회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_AND_DEPOSIT_DECISION
    ),
    SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED(
            "소규모 매수 10% 미만·매도 20% 미만 제외·동일 Tick 1회·급등 매수 최대 2회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_SIZE_SEPARATED_BULL_BUY_WITH_SMALL_TRADE_DEAD_ZONE
    ),
    CRASH_HOLDING_WITH_SMALL_TRADE_DEAD_ZONE(
            "소규모 거래 제외·급락 구간 주식 50% 이상 보유 유지",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_HOLDING
    ),
    NORMAL_PLANNED_BUY_WITH_SMALL_TRADE_DEAD_ZONE(
            "소규모 거래 제외·평범장 총자산 10~29% 계획 매수",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_NORMAL_BUY
    ),
    CASH_BUFFER_WITH_SMALL_TRADE_DEAD_ZONE(
            "소규모 거래 제외·현금 비중 25~49% 3 Tick 유지",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CASH_BUFFER
    ),
    CRASH_PARTIAL_SELL_WITH_SMALL_TRADE_DEAD_ZONE(
            "소규모 거래 제외·급락장 일부 매도 LH·RP 중심",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition
                    .EXCLUSIVE_MODERATE_SIZE_SEPARATED_WITH_SMALL_TRADE_DEAD_ZONE_AND_CRASH_PARTIAL_SELL
    ),
    RECOMMENDED_GAME_REPETITION_POLICY(
            "동일 Tick 1회·지배적 매수 규칙 상한·연속 배율 제거",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.DOMINANT_BUY_RULE_HARD_CAP,
            GameRuleEvaluationCondition.RECOMMENDED_GAME_REPETITION_POLICY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    THREE_CANDIDATE_RULES(
            "급락 보유·평범장 계획 매수·현금 완충 유지",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.BULL_BUY_MEDIAN_HARD_CAP,
            GameRuleEvaluationCondition.THREE_CANDIDATE_RULES
    ),
    RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES(
            "반복 정책과 세 후보 규칙 동시 적용",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.DOMINANT_BUY_RULE_HARD_CAP,
            GameRuleEvaluationCondition
                    .RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    OPPORTUNITY_WEIGHTED_REPETITION_POLICY(
            "반복 행동 기회 비율·관측 신뢰도 보정",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.OPPORTUNITY_WEIGHTED_REPETITION_POLICY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    LOG_DIMINISHING_CANDIDATE_RULES(
            "반복 정책·세 후보 규칙 P95 로그 감쇠",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.DOMINANT_BUY_RULE_HARD_CAP,
            GameRuleEvaluationCondition.LOG_DIMINISHING_CANDIDATE_RULES,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    LOG_DIMINISHING_ALL_REPEATED_RULES(
            "모든 반복 매수·매도와 세 후보 규칙 P95 로그 감쇠",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.LOG_DIMINISHING_ALL_REPEATED_RULES,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    LOG_DIMINISHING_RULE_GROUPS(
            "매수·매도·상태 유지 계열별 P95 로그 감쇠",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.LOG_DIMINISHING_RULE_GROUPS,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP(
            "계열별 로그 감쇠·매수매도 2.5회·상태 유지 1.5회분",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_RISK_BUDGET_MAINTENANCE(
            "균형 상한·위험 예산 유지",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_RISK_BUDGET_MAINTENANCE,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_PASSIVE_HIGH_RISK_HOLDING(
            "균형 상한·고위험 수동 보유",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_PASSIVE_HIGH_RISK_HOLDING,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_LIQUIDITY_PRESERVING_OPPORTUNITY(
            "균형 상한·유동성 보존 기회 실행",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_LIQUIDITY_PRESERVING_OPPORTUNITY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_RISK_BUDGET_AND_LIQUIDITY_OPPORTUNITY(
            "균형 상한·위험 예산 유지·유동성 보존 기회 실행",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .BALANCED_CAP_WITH_RISK_BUDGET_AND_LIQUIDITY_OPPORTUNITY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_RISK_BUDGET_AND_PASSIVE_HIGH_RISK(
            "균형 상한·무거래 위험 예산 유지·고주식 저현금 급락 무거래",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_RISK_BUDGET_AND_PASSIVE_HIGH_RISK,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_RISK_BUDGET_AND_NO_CHASE(
            "균형 상한·무거래 위험 예산 유지·위험 노출 기회 비추격",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_RISK_BUDGET_AND_NO_CHASE,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_RISK_BUDGET_AND_HIGH_STOCK_INACTIVITY(
            "균형 상한·무거래 위험 예산 유지·고주식 저현금 5 Tick 무거래",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .BALANCED_CAP_WITH_RISK_BUDGET_AND_HIGH_STOCK_INACTIVITY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_RISK_BUDGET_NO_CHASE_AND_HIGH_STOCK_INACTIVITY(
            "균형 상한·위험 예산·기회 비추격·고주식 저현금 무거래",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .BALANCED_CAP_WITH_RISK_BUDGET_NO_CHASE_AND_HIGH_STOCK_INACTIVITY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_ALL_NEW_RULES(
            "균형 상한·세 신규 규칙",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_ALL_NEW_RULES,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_REFINED_NO_CHASE(
            "균형 상한·위험 예산·정제된 기회 비추격",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_REFINED_NO_CHASE,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_REFINED_NO_CHASE_AND_LHH_OPPORTUNITY(
            "균형 상한·위험 예산·정제 비추격·LHH 유동성 기회 실행",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .BALANCED_CAP_WITH_REFINED_NO_CHASE_AND_LHH_OPPORTUNITY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_REFINED_NO_CHASE_AND_LHH_OPPORTUNITY_EXCLUSIVE(
            "균형 상한·위험 예산·정제 비추격·LHH 기회 실행 상호 배타",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition
                    .BALANCED_CAP_WITH_REFINED_NO_CHASE_AND_LHH_OPPORTUNITY_EXCLUSIVE,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_HHL_COMPOSITE(
            "균형 상한·위험 예산·HHL 복합 규칙",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_HHL_COMPOSITE,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_HLL_CAPPED_NO_CHASE(
            "균형 상한·위험 예산·HLL 정제 비추격 최대 1회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_HLL_CAPPED_NO_CHASE,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_LHH_COMPLETED_OPPORTUNITY(
            "균형 상한·위험 예산·LHH 완결형 기회 실행 최대 1회",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_LHH_COMPLETED_OPPORTUNITY,
            ConsecutiveActionMultiplierCondition.DISABLED
    ),
    BALANCED_CAP_WITH_ALL_TARGETED_RULES(
            "균형 상한·위험 예산·HHL·HLL·LHH 표적 규칙",
            SameTickRuleApplicationCondition.ONCE_PER_TICK,
            RuleAccumulationCondition.UNLIMITED,
            GameRuleEvaluationCondition.BALANCED_CAP_WITH_ALL_TARGETED_RULES,
            ConsecutiveActionMultiplierCondition.DISABLED
    );

    private final String description;
    private final SameTickRuleApplicationCondition sameTickRuleCondition;
    private final RuleAccumulationCondition ruleAccumulationCondition;
    private final GameRuleEvaluationCondition ruleEvaluationCondition;
    private final ConsecutiveActionMultiplierCondition multiplierCondition;

    GameBiasMitigationCondition(
            String description,
            SameTickRuleApplicationCondition sameTickRuleCondition,
            RuleAccumulationCondition ruleAccumulationCondition,
            GameRuleEvaluationCondition ruleEvaluationCondition) {
        this(
                description,
                sameTickRuleCondition,
                ruleAccumulationCondition,
                ruleEvaluationCondition,
                ConsecutiveActionMultiplierCondition.ENABLED
        );
    }

    GameBiasMitigationCondition(
            String description,
            SameTickRuleApplicationCondition sameTickRuleCondition,
            RuleAccumulationCondition ruleAccumulationCondition,
            GameRuleEvaluationCondition ruleEvaluationCondition,
            ConsecutiveActionMultiplierCondition multiplierCondition) {
        this.description = description;
        this.sameTickRuleCondition = sameTickRuleCondition;
        this.ruleAccumulationCondition = ruleAccumulationCondition;
        this.ruleEvaluationCondition = ruleEvaluationCondition;
        this.multiplierCondition = multiplierCondition;
    }
}
