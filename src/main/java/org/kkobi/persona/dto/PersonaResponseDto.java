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
    private BigDecimal stockRatio;
    private BigDecimal bondRatio;
    private BigDecimal depositRatio;
}