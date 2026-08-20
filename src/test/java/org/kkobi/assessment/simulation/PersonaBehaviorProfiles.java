package org.kkobi.assessment.simulation;

import org.kkobi.assessment.enums.PersonaType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PersonaBehaviorProfiles {

    private static final Map<PersonaType, PersonaBehaviorProfile> PROFILES = createProfiles();

    private PersonaBehaviorProfiles() {
    }

    public static PersonaBehaviorProfile get(PersonaType personaType) {
        PersonaBehaviorProfile profile = PROFILES.get(personaType);
        if (profile == null) {
            throw new IllegalArgumentException("지원하지 않는 성향입니다: " + personaType);
        }
        return profile;
    }

    public static List<PersonaBehaviorProfile> values() {
        return java.util.Arrays.stream(PersonaType.values())
                .map(PROFILES::get)
                .toList();
    }

    private static Map<PersonaType, PersonaBehaviorProfile> createProfiles() {
        EnumMap<PersonaType, PersonaBehaviorProfile> profiles =
                new EnumMap<>(PersonaType.class);
        profiles.put(PersonaType.HHH, profile(PersonaType.HHH, 75, 90, 0, 5,
                100, 80, 60, 100, 100, 50, 50, 65,
                80, 100, 100, 95, 70, 45, 100, 0, 100, 0));
        profiles.put(PersonaType.HHL, profile(PersonaType.HHL, 60, 65, 5, 10,
                75, 40, 95, 5, 95, 5, 95, 15,
                0, 100, 100, 100, 30, 100, 95, 0, 50, 50));
        profiles.put(PersonaType.HLH, profile(PersonaType.HLH, 75, 95, 0, 5,
                85, 95, 5, 95, 5, 90, 5, 95,
                95, 0, 0, 90, 95, 0, 5, 0, 20, 80));
        profiles.put(PersonaType.HLL, profile(PersonaType.HLL, 80, 85, 0, 5,
                45, 80, 55, 30, 50, 10, 15, 10,
                0, 80, 10, 95, 80, 80, 5, 0, 30, 70));
        profiles.put(PersonaType.LHH, profile(PersonaType.LHH, 10, 20, 40, 55,
                80, 0, 80, 80, 80, 80, 80, 40,
                30, 70, 100, 0, 0, 90, 100, 0, 100, 0));
        profiles.put(PersonaType.LHL, profile(PersonaType.LHL, 0, 15, 45, 70,
                35, 0, 95, 0, 95, 5, 95, 5,
                0, 100, 100, 0, 0, 95, 10, 25, 60, 15));
        profiles.put(PersonaType.LLH, profile(PersonaType.LLH, 10, 15, 55, 60,
                90, 0, 0, 100, 0, 100, 0, 100,
                100, 0, 0, 0, 0, 0, 0, 0, 100, 0));
        profiles.put(PersonaType.LLL, profile(PersonaType.LLL, 0, 10, 70, 90,
                10, 0, 10, 0, 10, 0, 10, 0,
                0, 10, 10, 0, 0, 10, 0, 80, 20, 0));
        return Map.copyOf(profiles);
    }

    private static PersonaBehaviorProfile profile(
            PersonaType personaType,
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
            int depositCancelProbability,
            int depositCancelThenBuyProbability,
            int depositCashRetentionProbability,
            int cashBufferMaintenanceProbability,
            int crashHoldingProbability,
            int lossAveragingProbability,
            int lossCutProbability,
            int profitTakingProbability,
            int smallTradeProbability,
            int mediumTradeProbability,
            int largeTradeProbability) {
        return new PersonaBehaviorProfile(
                personaType,
                minimumStockRatio,
                maximumStockRatio,
                minimumDepositRatio,
                maximumDepositRatio,
                actionProbability,
                crashBuyProbability,
                crashSellProbability,
                bullBuyProbability,
                bullSellProbability,
                normalBuyProbability,
                normalSellProbability,
                depositCancelProbability,
                depositCancelThenBuyProbability,
                depositCashRetentionProbability,
                cashBufferMaintenanceProbability,
                crashHoldingProbability,
                lossAveragingProbability,
                lossCutProbability,
                profitTakingProbability,
                smallTradeProbability,
                mediumTradeProbability,
                largeTradeProbability
        );
    }
}
