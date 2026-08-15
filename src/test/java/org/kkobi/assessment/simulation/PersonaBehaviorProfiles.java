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
        profiles.put(PersonaType.HHH, profile(PersonaType.HHH, 60, 85, 0, 20,
                70, 80, 15, 75, 25, 60, 25, 70));
        profiles.put(PersonaType.HHL, profile(PersonaType.HHL, 45, 70, 10, 35,
                55, 60, 30, 35, 65, 35, 55, 45));
        profiles.put(PersonaType.HLH, profile(PersonaType.HLH, 55, 80, 0, 20,
                65, 75, 20, 80, 20, 65, 20, 75));
        profiles.put(PersonaType.HLL, profile(PersonaType.HLL, 35, 60, 15, 40,
                45, 60, 30, 30, 70, 30, 60, 40));
        profiles.put(PersonaType.LHH, profile(PersonaType.LHH, 20, 45, 30, 60,
                40, 20, 75, 40, 60, 30, 65, 15));
        profiles.put(PersonaType.LHL, profile(PersonaType.LHL, 10, 35, 40, 70,
                30, 15, 80, 15, 80, 15, 75, 10));
        profiles.put(PersonaType.LLH, profile(PersonaType.LLH, 30, 55, 20, 45,
                45, 25, 70, 70, 30, 55, 35, 35));
        profiles.put(PersonaType.LLL, profile(PersonaType.LLL, 5, 25, 55, 85,
                20, 5, 90, 5, 90, 10, 85, 5));
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
            int depositCancelProbability) {
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
                depositCancelProbability
        );
    }
}
