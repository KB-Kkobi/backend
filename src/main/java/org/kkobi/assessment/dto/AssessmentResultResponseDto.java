package org.kkobi.assessment.dto;

import lombok.Data;
import org.kkobi.persona.dto.PersonaResponseDto;

import java.math.BigDecimal;

@Data
public class AssessmentResultResponseDto {

    private Long resultId;
    private Long userId;
    private BigDecimal rtScore;
    private BigDecimal lhScore;
    private BigDecimal rpScore;
    private PersonaResponseDto persona;
}