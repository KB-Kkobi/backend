package org.kkobi.assessment.simulation;

import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;

public class PersonaRuleApplicationAnalysis {

    private static final int STATISTICS_SCALE = 4;
    private static final int RATE_SCALE = 2;
    private static final BigDecimal TOTAL_RATE = BigDecimal.valueOf(100);

    private final EnumMap<PersonaType, MutablePersonaRuleStatistics> statisticsByPersona =
            new EnumMap<>(PersonaType.class);

    public PersonaRuleApplicationAnalysis() {
        for (PersonaType personaType : PersonaType.values()) {
            statisticsByPersona.put(
                    personaType,
                    new MutablePersonaRuleStatistics(personaType)
            );
        }
    }

    public void addResult(GameBehaviorSimulationResult simulationResult) {
        if (simulationResult == null) {
            throw new IllegalArgumentException("시뮬레이션 결과는 필수입니다.");
        }
        statisticsByPersona.get(simulationResult.getPersonaType())
                .addResult(simulationResult);
    }

    public List<RuleSummary> getRuleSummaries(PersonaType personaType) {
        if (personaType == null) {
            throw new IllegalArgumentException("성향 코드는 필수입니다.");
        }
        return statisticsByPersona.get(personaType).createRuleSummaries();
    }

    public int getPersonaUserCount(PersonaType personaType) {
        if (personaType == null) {
            throw new IllegalArgumentException("성향 코드는 필수입니다.");
        }
        return statisticsByPersona.get(personaType).userCount;
    }

    public record RuleSummary(
            BehaviorRuleCode ruleCode,
            long applicationCount,
            int appliedUserCount,
            BigDecimal appliedUserRate,
            BigDecimal averageApplicationCountPerUser,
            BigDecimal rtTotalContribution,
            BigDecimal lhTotalContribution,
            BigDecimal rpTotalContribution,
            BigDecimal rtAverageContributionPerUser,
            BigDecimal lhAverageContributionPerUser,
            BigDecimal rpAverageContributionPerUser) {
    }

    private static class MutablePersonaRuleStatistics {

        private final PersonaType personaType;
        private final EnumMap<BehaviorRuleCode, MutableRuleStatistics> ruleStatistics =
                new EnumMap<>(BehaviorRuleCode.class);
        private int userCount;

        private MutablePersonaRuleStatistics(PersonaType personaType) {
            this.personaType = personaType;
            for (BehaviorRuleCode ruleCode : BehaviorRuleCode.values()) {
                ruleStatistics.put(ruleCode, new MutableRuleStatistics(ruleCode));
            }
        }

        private void addResult(GameBehaviorSimulationResult simulationResult) {
            if (simulationResult.getPersonaType() != personaType) {
                throw new IllegalArgumentException("성향과 시뮬레이션 결과가 일치하지 않습니다.");
            }
            userCount++;
            for (MutableRuleStatistics statistics : ruleStatistics.values()) {
                statistics.addResult(simulationResult);
            }
        }

        private List<RuleSummary> createRuleSummaries() {
            List<RuleSummary> summaries = new ArrayList<>();
            for (MutableRuleStatistics statistics : ruleStatistics.values()) {
                summaries.add(statistics.createRuleSummary(userCount));
            }
            return summaries.stream()
                    .sorted(Comparator
                            .comparingLong(RuleSummary::applicationCount)
                            .reversed()
                            .thenComparing(summary -> summary.ruleCode().name()))
                    .toList();
        }
    }

    private static class MutableRuleStatistics {

        private final BehaviorRuleCode ruleCode;
        private long applicationCount;
        private int appliedUserCount;
        private ScoreDelta totalContribution = ScoreDelta.createZeroScoreDelta();

        private MutableRuleStatistics(BehaviorRuleCode ruleCode) {
            this.ruleCode = ruleCode;
        }

        private void addResult(GameBehaviorSimulationResult simulationResult) {
            int userApplicationCount = simulationResult.getRuleApplicationCounts()
                    .getOrDefault(ruleCode, 0);
            applicationCount += userApplicationCount;
            if (userApplicationCount > 0) {
                appliedUserCount++;
            }
            ScoreDelta userContribution = simulationResult.getRuleScoreContributions()
                    .get(ruleCode);
            if (userContribution != null) {
                totalContribution = totalContribution.addScoreDelta(userContribution);
            }
        }

        private RuleSummary createRuleSummary(int userCount) {
            return new RuleSummary(
                    ruleCode,
                    applicationCount,
                    appliedUserCount,
                    calculateRate(appliedUserCount, userCount),
                    calculateAverage(applicationCount, userCount),
                    totalContribution.getRtDelta(),
                    totalContribution.getLhDelta(),
                    totalContribution.getRpDelta(),
                    calculateAverage(totalContribution.getRtDelta(), userCount),
                    calculateAverage(totalContribution.getLhDelta(), userCount),
                    calculateAverage(totalContribution.getRpDelta(), userCount)
            );
        }

        private BigDecimal calculateRate(long value, int count) {
            if (count == 0) {
                return BigDecimal.ZERO.setScale(RATE_SCALE);
            }
            return BigDecimal.valueOf(value)
                    .multiply(TOTAL_RATE)
                    .divide(BigDecimal.valueOf(count), RATE_SCALE, RoundingMode.HALF_UP);
        }

        private BigDecimal calculateAverage(long value, int count) {
            return calculateAverage(BigDecimal.valueOf(value), count);
        }

        private BigDecimal calculateAverage(BigDecimal value, int count) {
            if (count == 0) {
                return BigDecimal.ZERO.setScale(STATISTICS_SCALE);
            }
            return value.divide(
                    BigDecimal.valueOf(count),
                    STATISTICS_SCALE,
                    RoundingMode.HALF_UP
            );
        }
    }
}
