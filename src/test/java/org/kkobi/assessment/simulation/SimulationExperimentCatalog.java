package org.kkobi.assessment.simulation;

import java.util.ArrayList;
import java.util.List;

public enum SimulationExperimentCatalog {

    SMALL_TRADE_DEAD_ZONE_FREQUENCY(
            "소규모 거래 제외 전후의 저·중·고빈도 비교",
            createSmallTradeDeadZoneFrequencyCases()
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
