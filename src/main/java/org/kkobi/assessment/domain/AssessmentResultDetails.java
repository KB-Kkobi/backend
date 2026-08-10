package org.kkobi.assessment.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AssessmentResultDetails {

    private Long resultId;
    private String axisCode;
    private String personaName;
    private String description;
    private String feature;
    private String strength;
    private String caution;
    private BigDecimal rtScore;
    private BigDecimal lhScore;
    private BigDecimal rpScore;
    private BigDecimal stockRatio;
    private BigDecimal bondRatio;
    private BigDecimal depositRatio;
    private LocalDateTime analyzedAt;
}
