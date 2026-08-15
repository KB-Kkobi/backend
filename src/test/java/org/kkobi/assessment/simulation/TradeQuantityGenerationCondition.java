package org.kkobi.assessment.simulation;

import lombok.Getter;

@Getter
public enum TradeQuantityGenerationCondition {

    CURRENT_RANDOM_BUY_PERCENTAGE("기존 매수 1~100%·매도 25%/50%/100%"),
    SYMMETRIC_THREE_LEVEL("매수·매도 모두 25%/50%/100%");

    private final String description;

    TradeQuantityGenerationCondition(String description) {
        this.description = description;
    }
}
