package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.PersonaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaBehaviorProfilesTest {

    @Test
    @DisplayName("HHH부터 LLL까지 모든 유형의 행동 프로필을 제공한다.")
    void provideEveryPersonaProfile() {
        assertEquals(PersonaType.values().length, PersonaBehaviorProfiles.values().size());
        for (PersonaType personaType : PersonaType.values()) {
            PersonaBehaviorProfile profile = PersonaBehaviorProfiles.get(personaType);
            assertEquals(personaType, profile.targetPersona());
            assertTrue(profile.minimumStockRatio() <= profile.maximumStockRatio());
            assertTrue(profile.minimumDepositRatio() <= profile.maximumDepositRatio());
            assertTrue(profile.depositCashRetentionProbability() >= 0);
            assertTrue(profile.cashBufferMaintenanceProbability() >= 0);
        }
    }

    @Test
    @DisplayName("유동성 선호 유형은 예금 해지 후 현금 유지와 현금 완충 유지 확률이 더 높다.")
    void distinguishCashMaintenanceBehaviorByPersona() {
        PersonaBehaviorProfile highLiquidity = PersonaBehaviorProfiles.get(PersonaType.LHL);
        PersonaBehaviorProfile lowLiquidity = PersonaBehaviorProfiles.get(PersonaType.HLH);

        assertTrue(highLiquidity.depositCashRetentionProbability()
                > lowLiquidity.depositCashRetentionProbability());
        assertTrue(highLiquidity.cashBufferMaintenanceProbability()
                > lowLiquidity.cashBufferMaintenanceProbability());
    }
}
