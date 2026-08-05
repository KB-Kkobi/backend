package org.kkobi.assessment.enums;

public enum MarketState {
    CRASH,
    BULL,
    VOLATILE,
    NORMAL;

    public static MarketState getMarketState(String marketState) {
        return switch (marketState) {
            case "CRASH" -> CRASH;
            case "BULL" -> BULL;
            case "VOLATILE" -> VOLATILE;
            case "NORMAL", "BEAR", "SIDEWAYS" -> NORMAL;
            default -> throw new IllegalArgumentException("지원하지 않는 시장 상태입니다: " + marketState);
        };
    }
}
