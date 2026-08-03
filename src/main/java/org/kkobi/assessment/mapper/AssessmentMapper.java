package org.kkobi.assessment.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.assessment.domain.AssessmentScore;

public interface AssessmentMapper {

    AssessmentScore getLatestAssessmentScore(@Param("userId") Long userId);

    Long getPersonaIdByName(@Param("personaName") String personaName);

    int saveAssessmentResult(
            @Param("userId") Long userId,
            @Param("personaId") Long personaId,
            @Param("score") AssessmentScore assessmentScore
    );
}
