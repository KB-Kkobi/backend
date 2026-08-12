package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.RuleResult;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.assessment.mapper.AssessmentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentResultService {

    private final AssessmentMapper assessmentMapper;
    private final PersonaClassifier personaClassifier;

    public boolean existsAssessmentResult(Long userId) {
        return assessmentMapper.getLatestAssessmentScore(userId) != null;
    }

    public AssessmentScore getLatestAssessmentScore(Long userId) {
        AssessmentScore assessmentScore = assessmentMapper.getLatestAssessmentScore(userId);
        return assessmentScore == null
                ? AssessmentScore.createInitialScore()
                : assessmentScore;
    }

    public AssessmentResultDetails getLatestAssessmentResultDetails(Long userId) {
        return assessmentMapper.getLatestAssessmentResultDetails(userId);
    }
    
    public AssessmentResultResponseDto getLatestAssessmentResult(Long userId) {
        return assessmentMapper.getLatestAssessmentResult(userId);
    }

    public AssessmentResult createAssessmentResult(
            AssessmentScore assessmentScore,
            List<RuleResult> appliedRules) {
        PersonaType personaType = personaClassifier.calculatePersona(assessmentScore);
        return new AssessmentResult(assessmentScore, personaType, List.copyOf(appliedRules));
    }

    public AssessmentResult saveAssessmentResult(
            Long userId,
            AssessmentScore assessmentScore,
            List<RuleResult> appliedRules) {
        PersonaType personaType = personaClassifier.calculatePersona(assessmentScore);
        Long personaId = assessmentMapper.getPersonaIdByAxisCode(personaType.getAxisCode());
        if (personaId == null) {
            throw new IllegalStateException("투자 성향 기준 정보를 찾을 수 없습니다: " + personaType.getAxisCode());
        }

        int savedRowCount = assessmentMapper.saveAssessmentResult(
                userId,
                personaId,
                assessmentScore
        );
        if (savedRowCount != 1) {
            throw new IllegalStateException("투자 성향 결과를 저장하지 못했습니다.");
        }

        return new AssessmentResult(assessmentScore, personaType, List.copyOf(appliedRules));
    }
}
