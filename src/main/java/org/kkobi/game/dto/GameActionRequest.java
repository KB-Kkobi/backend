package org.kkobi.game.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class GameActionRequest {

    @NotNull
    @Min(0)
    private Integer gameTick;

    @NotBlank
    private String actionType;

    @NotBlank
    private String assetType;

    @NotNull
    @Min(0)
    private Long actionAmount;

    @NotNull
    @Min(0)
    private Long currentCash;

    @NotNull
    @Min(0)
    private Long currentStockPrincipal;

    @NotNull
    @Min(0)
    private Long currentDeposit;
}
