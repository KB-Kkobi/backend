package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;

import java.util.Objects;

public record PersonaBehaviorProfile(
        PersonaType targetPersona,
        int minimumStockRatio,
        int maximumStockRatio,
        int minimumDepositRatio,
        int maximumDepositRatio,
        int actionProbability,
        int crashBuyProbability,
        int crashSellProbability,
        int bullBuyProbability,
        int bullSellProbability,
        int normalBuyProbability,
        int normalSellProbability,
        int depositCancelProbability) {

    public PersonaBehaviorProfile {
        Objects.requireNonNull(targetPersona, "목표 성향은 필수입니다.");
        validateRange(minimumStockRatio, maximumStockRatio, "주식 비율");
        validateRange(minimumDepositRatio, maximumDepositRatio, "예금 비율");
        validateProbability(actionProbability, "행동 확률");
        validateProbability(crashBuyProbability, "급락장 매수 확률");
        validateProbability(crashSellProbability, "급락장 매도 확률");
        validateProbability(bullBuyProbability, "급등장 매수 확률");
        validateProbability(bullSellProbability, "급등장 매도 확률");
        validateProbability(normalBuyProbability, "평범장 매수 확률");
        validateProbability(normalSellProbability, "평범장 매도 확률");
        validateProbability(depositCancelProbability, "예금 해지 확률");
        if (minimumStockRatio + minimumDepositRatio > 100) {
            throw new IllegalArgumentException("최소 자산 배분 비율의 합은 100을 초과할 수 없습니다.");
        }
    }

    private static void validateRange(int minimum, int maximum, String name) {
        if (minimum < 0 || maximum > 100 || minimum > maximum) {
            throw new IllegalArgumentException(name + " 범위가 올바르지 않습니다.");
        }
    }

    private static void validateProbability(int probability, String name) {
        if (probability < 0 || probability > 100) {
            throw new IllegalArgumentException(name + "은 0 이상 100 이하여야 합니다.");
        }
    }
}
