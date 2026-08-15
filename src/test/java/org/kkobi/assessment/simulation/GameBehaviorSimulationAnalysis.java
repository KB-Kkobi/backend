package org.kkobi.assessment.simulation;

import lombok.Getter;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class GameBehaviorSimulationAnalysis {

    private final int totalSimulationCount;
    private final Map<PersonaType, PersonaSummary> personaSummaries;
    private final ScoreSummary overallRtScoreSummary;
    private final ScoreSummary overallLhScoreSummary;
    private final ScoreSummary overallRpScoreSummary;
    private final BehaviorStatistics behaviorStatistics;
    private final Map<BehaviorRuleCode, RuleStatistics> ruleStatistics;
    private final ScoreDiagnostic rtScoreDiagnostic;
    private final ScoreDiagnostic lhScoreDiagnostic;
    private final ScoreDiagnostic rpScoreDiagnostic;

    public GameBehaviorSimulationAnalysis(
            int totalSimulationCount,
            Map<PersonaType, PersonaSummary> personaSummaries,
            ScoreSummary overallRtScoreSummary,
            ScoreSummary overallLhScoreSummary,
            ScoreSummary overallRpScoreSummary,
            BehaviorStatistics behaviorStatistics,
            Map<BehaviorRuleCode, RuleStatistics> ruleStatistics,
            ScoreDiagnostic rtScoreDiagnostic,
            ScoreDiagnostic lhScoreDiagnostic,
            ScoreDiagnostic rpScoreDiagnostic) {
        if (totalSimulationCount <= 0) {
            throw new IllegalArgumentException("전체 시뮬레이션 수는 0보다 커야 합니다.");
        }
        Objects.requireNonNull(personaSummaries, "성향별 분석 결과는 필수입니다.");
        if (personaSummaries.size() != PersonaType.values().length) {
            throw new IllegalArgumentException("8가지 성향의 분석 결과가 모두 필요합니다.");
        }

        this.totalSimulationCount = totalSimulationCount;
        this.personaSummaries = copyPersonaSummaries(personaSummaries);
        this.overallRtScoreSummary = Objects.requireNonNull(
                overallRtScoreSummary,
                "전체 RT 점수 통계는 필수입니다."
        );
        this.overallLhScoreSummary = Objects.requireNonNull(
                overallLhScoreSummary,
                "전체 LH 점수 통계는 필수입니다."
        );
        this.overallRpScoreSummary = Objects.requireNonNull(
                overallRpScoreSummary,
                "전체 RP 점수 통계는 필수입니다."
        );
        this.behaviorStatistics = Objects.requireNonNull(
                behaviorStatistics,
                "전체 행동 통계는 필수입니다."
        );
        this.ruleStatistics = copyRuleStatistics(ruleStatistics);
        this.rtScoreDiagnostic = Objects.requireNonNull(
                rtScoreDiagnostic,
                "RT 점수 진단 결과는 필수입니다."
        );
        this.lhScoreDiagnostic = Objects.requireNonNull(
                lhScoreDiagnostic,
                "LH 점수 진단 결과는 필수입니다."
        );
        this.rpScoreDiagnostic = Objects.requireNonNull(
                rpScoreDiagnostic,
                "RP 점수 진단 결과는 필수입니다."
        );
    }

    public PersonaSummary getPersonaSummary(PersonaType personaType) {
        Objects.requireNonNull(personaType, "성향 코드는 필수입니다.");
        return personaSummaries.get(personaType);
    }

    private Map<PersonaType, PersonaSummary> copyPersonaSummaries(
            Map<PersonaType, PersonaSummary> summaries) {
        EnumMap<PersonaType, PersonaSummary> copiedSummaries = new EnumMap<>(PersonaType.class);
        for (PersonaType personaType : PersonaType.values()) {
            PersonaSummary personaSummary = summaries.get(personaType);
            if (personaSummary == null) {
                throw new IllegalArgumentException(
                        "성향 분석 결과가 누락되었습니다: " + personaType
                );
            }
            copiedSummaries.put(personaType, personaSummary);
        }
        return Map.copyOf(copiedSummaries);
    }

    private Map<BehaviorRuleCode, RuleStatistics> copyRuleStatistics(
            Map<BehaviorRuleCode, RuleStatistics> statistics) {
        Objects.requireNonNull(statistics, "행동 규칙 통계는 필수입니다.");
        EnumMap<BehaviorRuleCode, RuleStatistics> copiedStatistics =
                new EnumMap<>(BehaviorRuleCode.class);
        for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
            RuleStatistics ruleStatistic = statistics.get(ruleCode);
            if (ruleStatistic == null) {
                throw new IllegalArgumentException(
                        "행동 규칙 통계가 누락되었습니다: " + ruleCode
                );
            }
            copiedStatistics.put(ruleCode, ruleStatistic);
        }
        return Map.copyOf(copiedStatistics);
    }

    @Getter
    public static class PersonaSummary {

        private final PersonaType personaType;
        private final int simulationCount;
        private final BigDecimal distributionRate;
        private final ScoreSummary rtScoreSummary;
        private final ScoreSummary lhScoreSummary;
        private final ScoreSummary rpScoreSummary;

        PersonaSummary(
                PersonaType personaType,
                int simulationCount,
                BigDecimal distributionRate,
                ScoreSummary rtScoreSummary,
                ScoreSummary lhScoreSummary,
                ScoreSummary rpScoreSummary) {
            this.personaType = personaType;
            this.simulationCount = simulationCount;
            this.distributionRate = distributionRate;
            this.rtScoreSummary = rtScoreSummary;
            this.lhScoreSummary = lhScoreSummary;
            this.rpScoreSummary = rpScoreSummary;
        }
    }

    @Getter
    public static class ScoreSummary {

        private final BigDecimal average;
        private final BigDecimal standardDeviation;
        private final BigDecimal minimum;
        private final BigDecimal maximum;

        ScoreSummary(
                BigDecimal average,
                BigDecimal standardDeviation,
                BigDecimal minimum,
                BigDecimal maximum) {
            this.average = average;
            this.standardDeviation = standardDeviation;
            this.minimum = minimum;
            this.maximum = maximum;
        }
    }

    @Getter
    public static class BehaviorStatistics {

        private final BigDecimal averageBuyCount;
        private final BigDecimal averageSellCount;
        private final BigDecimal averageNoActionTickCount;
        private final BigDecimal averageActionCountPerTick;
        private final long consecutiveActionLevelTwoCount;
        private final long consecutiveActionLevelThreeOrMoreCount;

        BehaviorStatistics(
                BigDecimal averageBuyCount,
                BigDecimal averageSellCount,
                BigDecimal averageNoActionTickCount,
                BigDecimal averageActionCountPerTick,
                long consecutiveActionLevelTwoCount,
                long consecutiveActionLevelThreeOrMoreCount) {
            this.averageBuyCount = averageBuyCount;
            this.averageSellCount = averageSellCount;
            this.averageNoActionTickCount = averageNoActionTickCount;
            this.averageActionCountPerTick = averageActionCountPerTick;
            this.consecutiveActionLevelTwoCount = consecutiveActionLevelTwoCount;
            this.consecutiveActionLevelThreeOrMoreCount =
                    consecutiveActionLevelThreeOrMoreCount;
        }
    }

    @Getter
    public static class RuleStatistics {

        private final BehaviorRuleCode ruleCode;
        private final long applicationCount;
        private final BigDecimal rtTotalContribution;
        private final BigDecimal lhTotalContribution;
        private final BigDecimal rpTotalContribution;

        RuleStatistics(
                BehaviorRuleCode ruleCode,
                long applicationCount,
                BigDecimal rtTotalContribution,
                BigDecimal lhTotalContribution,
                BigDecimal rpTotalContribution) {
            this.ruleCode = ruleCode;
            this.applicationCount = applicationCount;
            this.rtTotalContribution = rtTotalContribution;
            this.lhTotalContribution = lhTotalContribution;
            this.rpTotalContribution = rpTotalContribution;
        }
    }

    @Getter
    public static class ScoreDiagnostic {

        private final int maximumScoreCount;
        private final BigDecimal maximumScoreRate;
        private final int boundaryScoreCount;
        private final BigDecimal boundaryScoreRate;

        ScoreDiagnostic(
                int maximumScoreCount,
                BigDecimal maximumScoreRate,
                int boundaryScoreCount,
                BigDecimal boundaryScoreRate) {
            this.maximumScoreCount = maximumScoreCount;
            this.maximumScoreRate = maximumScoreRate;
            this.boundaryScoreCount = boundaryScoreCount;
            this.boundaryScoreRate = boundaryScoreRate;
        }
    }
}
