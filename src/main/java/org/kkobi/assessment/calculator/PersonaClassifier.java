package org.kkobi.assessment.calculator;

import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.enums.PersonaType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PersonaClassifier {

    private static final BigDecimal HIGH_SCORE_THRESHOLD = BigDecimal.valueOf(50);

    public PersonaType calculatePersona(AssessmentScore assessmentScore) {
        boolean highRt = isHighScore(assessmentScore.getRtScore());
        boolean highLh = isHighScore(assessmentScore.getLhScore());
        boolean highRp = isHighScore(assessmentScore.getRpScore());

        if (highRt && highLh && highRp) {
            return PersonaType.HHH;
        }
        if (highRt && highLh) {
            return PersonaType.HHL;
        }
        if (highRt && highRp) {
            return PersonaType.HLH;
        }
        if (highRt) {
            return PersonaType.HLL;
        }
        if (highLh && highRp) {
            return PersonaType.LHH;
        }
        if (highLh) {
            return PersonaType.LHL;
        }
        if (highRp) {
            return PersonaType.LLH;
        }
        return PersonaType.LLL;
    }

    private boolean isHighScore(BigDecimal score) {
        return score.compareTo(HIGH_SCORE_THRESHOLD) >= 0;
    }
}
