package org.kkobi.game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GameStartResponse {

    private final Long seedMoney;
    private final Long cashAmount;
    private final Long stockAmount;
    private final Long depositAmount;
    private final Integer currentTick;
    private final Integer totalTick;
    private final String depositStatus;
}
