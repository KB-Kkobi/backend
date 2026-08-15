package org.kkobi.assessment.simulation;

import java.util.ArrayList;
import java.util.List;

public enum SimulationExperimentCatalog {

    SMALL_TRADE_DEAD_ZONE_FREQUENCY(
            "소규모 거래 제외 전후의 저·중·고빈도 비교",
            createSmallTradeDeadZoneFrequencyCases()
    ),
    CRASH_HOLDING_FREQUENCY(
            "급락 구간 보유 유지 규칙 적용 전후의 저·중·고빈도 비교",
            createCrashHoldingFrequencyCases()
    ),
    NORMAL_PLANNED_BUY_FREQUENCY(
            "평범장 계획 매수 규칙 적용 전후의 저·중·고빈도 비교",
            createNormalPlannedBuyFrequencyCases()
    ),
    CASH_BUFFER_MAINTENANCE_FREQUENCY(
            "현금 완충 비중 유지 규칙 적용 전후의 저·중·고빈도 비교",
            createCashBufferMaintenanceFrequencyCases()
    ),
    CRASH_PARTIAL_SELL_FREQUENCY(
            "급락장 일부 매도 LH·RP 중심 규칙 적용 전후의 저·중·고빈도 비교",
            createCrashPartialSellFrequencyCases()
    ),
    RECOMMENDED_REPETITION_POLICY_FREQUENCY(
            "게임 반복 행동 권장 정책 적용 전후의 저·중·고빈도 비교",
            createRecommendedRepetitionPolicyFrequencyCases()
    ),
    REPETITION_POLICY_AND_CANDIDATE_RULES_FREQUENCY(
            "반복 정책·세 후보 규칙·동시 적용의 저·중·고빈도 비교",
            createRepetitionPolicyAndCandidateRulesFrequencyCases()
    ),
    OPPORTUNITY_WEIGHTED_REPETITION_FREQUENCY(
            "반복 행동 상한과 기회 비율·신뢰도 보정 방식 비교",
            createOpportunityWeightedRepetitionFrequencyCases()
    ),
    LOG_DIMINISHING_CANDIDATE_RULES_FREQUENCY(
            "세 후보 규칙 단순 누적과 P95 로그 감쇠 방식 비교",
            createLogDiminishingCandidateRulesFrequencyCases()
    ),
    LOG_DIMINISHING_ALL_REPEATED_RULES_FREQUENCY(
            "기존 반복 상한과 모든 반복 규칙 P95 로그 감쇠 방식 비교",
            createLogDiminishingAllRepeatedRulesFrequencyCases()
    ),
    LOG_DIMINISHING_RULE_GROUPS_FREQUENCY(
            "개별 반복 규칙과 매수·매도·상태 유지 계열 로그 감쇠 비교",
            createLogDiminishingRuleGroupsFrequencyCases()
    ),
    LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP_FREQUENCY(
            "계열별 로그 감쇠의 최대 기여도 1회분과 권장 중간안 비교",
            createLogDiminishingRuleGroupsBalancedCapFrequencyCases()
    );

    private final String description;
    private final List<SimulationExperimentCase> experimentCases;

    SimulationExperimentCatalog(
            String description,
            List<SimulationExperimentCase> experimentCases) {
        this.description = description;
        this.experimentCases = List.copyOf(experimentCases);
    }

    public String getDescription() {
        return description;
    }

    public List<SimulationExperimentCase> getExperimentCases() {
        return experimentCases;
    }

    private static List<SimulationExperimentCase> createSmallTradeDeadZoneFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "기존 편중 완화 후보",
                GameBiasMitigationCondition.SIZE_SEPARATED_BULL_BUY_ONCE_AND_CAPPED
        );
        addFrequencyCases(
                experimentCases,
                "소규모 매수·매도 점수 제외 후보",
                GameBiasMitigationCondition.SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase> createCrashHoldingFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "급락 구간 보유 유지 규칙 적용 전",
                GameBiasMitigationCondition.SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED
        );
        addFrequencyCases(
                experimentCases,
                "급락 구간 보유 유지 규칙 적용 후",
                GameBiasMitigationCondition.CRASH_HOLDING_WITH_SMALL_TRADE_DEAD_ZONE
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase> createNormalPlannedBuyFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "평범장 계획 매수 규칙 적용 전",
                GameBiasMitigationCondition.SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED
        );
        addFrequencyCases(
                experimentCases,
                "평범장 계획 매수 규칙 적용 후",
                GameBiasMitigationCondition.NORMAL_PLANNED_BUY_WITH_SMALL_TRADE_DEAD_ZONE
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase> createCashBufferMaintenanceFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "현금 완충 비중 유지 규칙 적용 전",
                GameBiasMitigationCondition.SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED
        );
        addFrequencyCases(
                experimentCases,
                "현금 완충 비중 유지 규칙 적용 후",
                GameBiasMitigationCondition.CASH_BUFFER_WITH_SMALL_TRADE_DEAD_ZONE
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase> createCrashPartialSellFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "급락장 일부 매도 LH·RP 중심 적용 전",
                GameBiasMitigationCondition.SMALL_TRADE_DEAD_ZONE_ONCE_AND_CAPPED
        );
        addFrequencyCases(
                experimentCases,
                "급락장 일부 매도 LH·RP 중심 적용 후",
                GameBiasMitigationCondition.CRASH_PARTIAL_SELL_WITH_SMALL_TRADE_DEAD_ZONE
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase> createRecommendedRepetitionPolicyFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "게임 반복 행동 권장 정책 적용 전",
                GameBiasMitigationCondition.CRASH_PARTIAL_SELL_WITH_SMALL_TRADE_DEAD_ZONE
        );
        addFrequencyCases(
                experimentCases,
                "게임 반복 행동 권장 정책 적용 후",
                GameBiasMitigationCondition.RECOMMENDED_GAME_REPETITION_POLICY
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase>
    createRepetitionPolicyAndCandidateRulesFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "최신 반복 정책만 적용",
                GameBiasMitigationCondition.RECOMMENDED_GAME_REPETITION_POLICY
        );
        addFrequencyCases(
                experimentCases,
                "세 후보 규칙만 적용",
                GameBiasMitigationCondition.THREE_CANDIDATE_RULES
        );
        addFrequencyCases(
                experimentCases,
                "최신 반복 정책과 세 후보 규칙 동시 적용",
                GameBiasMitigationCondition
                        .RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase>
    createOpportunityWeightedRepetitionFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "규칙별 반복 상한 방식",
                GameBiasMitigationCondition.RECOMMENDED_GAME_REPETITION_POLICY
        );
        addFrequencyCases(
                experimentCases,
                "행동 기회 비율·관측 신뢰도 보정 방식",
                GameBiasMitigationCondition.OPPORTUNITY_WEIGHTED_REPETITION_POLICY
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase>
    createLogDiminishingCandidateRulesFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "최신 반복 정책만 적용",
                GameBiasMitigationCondition.RECOMMENDED_GAME_REPETITION_POLICY
        );
        addFrequencyCases(
                experimentCases,
                "세 후보 규칙 단순 누적",
                GameBiasMitigationCondition
                        .RECOMMENDED_REPETITION_POLICY_AND_THREE_CANDIDATE_RULES
        );
        addFrequencyCases(
                experimentCases,
                "세 후보 규칙 P95 로그 감쇠",
                GameBiasMitigationCondition.LOG_DIMINISHING_CANDIDATE_RULES
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase>
    createLogDiminishingAllRepeatedRulesFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "기존 매수 상한과 후보 규칙 로그 감쇠",
                GameBiasMitigationCondition.LOG_DIMINISHING_CANDIDATE_RULES
        );
        addFrequencyCases(
                experimentCases,
                "모든 반복 매수·매도와 후보 규칙 로그 감쇠",
                GameBiasMitigationCondition.LOG_DIMINISHING_ALL_REPEATED_RULES
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase>
    createLogDiminishingRuleGroupsFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "후보 규칙만 로그 감쇠",
                GameBiasMitigationCondition.LOG_DIMINISHING_CANDIDATE_RULES
        );
        addFrequencyCases(
                experimentCases,
                "반복 규칙별 로그 감쇠",
                GameBiasMitigationCondition.LOG_DIMINISHING_ALL_REPEATED_RULES
        );
        addFrequencyCases(
                experimentCases,
                "매수·매도·상태 유지 계열별 로그 감쇠",
                GameBiasMitigationCondition.LOG_DIMINISHING_RULE_GROUPS
        );
        return experimentCases;
    }

    private static List<SimulationExperimentCase>
    createLogDiminishingRuleGroupsBalancedCapFrequencyCases() {
        List<SimulationExperimentCase> experimentCases = new ArrayList<>();
        addFrequencyCases(
                experimentCases,
                "후보 규칙만 로그 감쇠",
                GameBiasMitigationCondition.LOG_DIMINISHING_CANDIDATE_RULES
        );
        addFrequencyCases(
                experimentCases,
                "계열별 최대 평균 1회분",
                GameBiasMitigationCondition.LOG_DIMINISHING_RULE_GROUPS
        );
        addFrequencyCases(
                experimentCases,
                "매수매도 최대 2.5회·상태 유지 최대 1.5회분",
                GameBiasMitigationCondition.LOG_DIMINISHING_RULE_GROUPS_BALANCED_CAP
        );
        return experimentCases;
    }

    private static void addFrequencyCases(
            List<SimulationExperimentCase> experimentCases,
            String description,
            GameBiasMitigationCondition mitigationCondition) {
        for (GameBehaviorFrequencyCondition frequencyCondition
                : GameBehaviorFrequencyCondition.values()) {
            experimentCases.add(new SimulationExperimentCase(
                    mitigationCondition.name() + "_" + frequencyCondition.name(),
                    description,
                    mitigationCondition,
                    frequencyCondition
            ));
        }
    }
}
