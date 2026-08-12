package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    @Test
    @DisplayName("50점은 High이며 49.99점과 50.01점을 정확히 구분한다.")
    void calculatePersonaAtHighLowBoundary() {
        assertEquals(PersonaType.LLL, personaClassifier.calculatePersona(
                createScore("49.99", "49.99", "49.99")
        ));
        assertEquals(PersonaType.HLL, personaClassifier.calculatePersona(
                createScore("50.00", "49.99", "49.99")
        ));
        assertEquals(PersonaType.HLL, personaClassifier.calculatePersona(
                createScore("50.01", "49.99", "49.99")
        ));

        assertEquals(PersonaType.LLL, personaClassifier.calculatePersona(
                createScore("49.99", "49.99", "49.99")
        ));
        assertEquals(PersonaType.LHL, personaClassifier.calculatePersona(
                createScore("49.99", "50.00", "49.99")
        ));
        assertEquals(PersonaType.LHL, personaClassifier.calculatePersona(
                createScore("49.99", "50.01", "49.99")
        ));

        assertEquals(PersonaType.LLL, personaClassifier.calculatePersona(
                createScore("49.99", "49.99", "49.99")
        ));
        assertEquals(PersonaType.LLH, personaClassifier.calculatePersona(
                createScore("49.99", "49.99", "50.00")
        ));
        assertEquals(PersonaType.LLH, personaClassifier.calculatePersona(
                createScore("49.99", "49.99", "50.01")
        ));
    }

    @Test
    @DisplayName("High와 Low의 모든 조합은 8가지 성향 유형에 빠짐없이 매핑된다.")
    void calculateEveryPersonaTypeWithoutMappingBias() {
        Set<PersonaType> calculatedPersonas = EnumSet.noneOf(PersonaType.class);
        String[] boundaryScores = {"49.99", "50.01"};

        for (String rtScore : boundaryScores) {
            for (String lhScore : boundaryScores) {
                for (String rpScore : boundaryScores) {
                    calculatedPersonas.add(personaClassifier.calculatePersona(
                            createScore(rtScore, lhScore, rpScore)
                    ));
                }
            }
        }

        assertEquals(EnumSet.allOf(PersonaType.class), calculatedPersonas);
    }

    private AssessmentScore createScore(int rtScore, int lhScore, int rpScore) {
        return new AssessmentScore(
                BigDecimal.valueOf(rtScore),
                BigDecimal.valueOf(lhScore),
                BigDecimal.valueOf(rpScore)
        );
    }

    private AssessmentScore createScore(String rtScore, String lhScore, String rpScore) {
        return new AssessmentScore(
                new BigDecimal(rtScore),
                new BigDecimal(lhScore),
                new BigDecimal(rpScore)
        );
    }
}
