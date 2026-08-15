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
        }
    }
}
