package org.kkobi.assessment.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.enums.PersonaType;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class AssessmentResult {

    private final AssessmentScore assessmentScore;
    private final PersonaType personaType;
    private final List<RuleResult> appliedRules;
}
