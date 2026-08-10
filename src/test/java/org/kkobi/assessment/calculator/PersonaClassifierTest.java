package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonaClassifierTest {

    private final PersonaClassifier personaClassifier = new PersonaClassifier();

    @Test
    @DisplayName("RT, LH, RP의 High와 Low 조합을 8가지 투자 유형으로 판정한다.")
    void calculatePersona() {
        Map<AssessmentScore, PersonaType> expectedPersonas = new LinkedHashMap<>();
        expectedPersonas.put(createScore(50, 50, 50), PersonaType.HHH);
        expectedPersonas.put(createScore(50, 50, 49), PersonaType.HHL);
        expectedPersonas.put(createScore(50, 49, 50), PersonaType.HLH);
        expectedPersonas.put(createScore(50, 49, 49), PersonaType.HLL);
        expectedPersonas.put(createScore(49, 50, 50), PersonaType.LHH);
        expectedPersonas.put(createScore(49, 50, 49), PersonaType.LHL);
        expectedPersonas.put(createScore(49, 49, 50), PersonaType.LLH);
        expectedPersonas.put(createScore(49, 49, 49), PersonaType.LLL);

        expectedPersonas.forEach((assessmentScore, personaType) ->
                assertEquals(personaType, personaClassifier.calculatePersona(assessmentScore))
        );
    }

    private AssessmentScore createScore(int rtScore, int lhScore, int rpScore) {
        return new AssessmentScore(
                BigDecimal.valueOf(rtScore),
                BigDecimal.valueOf(lhScore),
                BigDecimal.valueOf(rpScore)
        );
    }
}
