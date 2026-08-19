package org.kkobi.persona.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PersonaResponseDto {

    private Long personaId;
    private String personaName;
    private String axisCode;
    private String imagePath;
    private String description;
    private String feature;
    private String strength;
    private String caution;
    private String reasonTitle;
    private String reasonPrefix;
    private String reasonHighlight;
    private String reasonSuffix;
    private String reasonSummary;
    private String portfolioReasonFirst;
    private String portfolioReasonSecond;
    private BigDecimal stockRatio;
    private BigDecimal bondRatio;
    private BigDecimal depositRatio;
}
