package org.kkobi.game.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class GameStartRequest {

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal cashRatio;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal stockRatio;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal depositRatio;
}
