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
            return PersonaType.FLAME_CHASER;
        }
        if (highRt && highLh) {
            return PersonaType.SMART_TRADER;
        }
        if (highRt && highRp) {
            return PersonaType.AMBITIOUS_PIONEER;
        }
        if (highRt) {
            return PersonaType.CONVICTION_VALUE_INVESTOR;
        }
        if (highLh && highRp) {
            return PersonaType.PRACTICAL_INFORMATION_SEEKER;
        }
        if (highLh) {
            return PersonaType.CASH_PRESERVER;
        }
        if (highRp) {
            return PersonaType.STEADY_ACCUMULATOR;
        }
        return PersonaType.STRICT_VAULT_KEEPER;
    }

    private boolean isHighScore(BigDecimal score) {
        return score.compareTo(HIGH_SCORE_THRESHOLD) >= 0;
    }
}
