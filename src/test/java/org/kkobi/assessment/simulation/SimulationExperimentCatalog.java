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
