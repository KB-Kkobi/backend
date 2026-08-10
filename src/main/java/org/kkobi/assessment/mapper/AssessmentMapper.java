package org.kkobi.assessment.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;

public interface AssessmentMapper {

    AssessmentScore getLatestAssessmentScore(@Param("userId") Long userId);

    AssessmentResultDetails getLatestAssessmentResultDetails(@Param("userId") Long userId);

    Long getPersonaIdByAxisCode(@Param("axisCode") String axisCode);

    int saveAssessmentResult(
            @Param("userId") Long userId,
            @Param("personaId") Long personaId,
            @Param("score") AssessmentScore assessmentScore
    );
}
