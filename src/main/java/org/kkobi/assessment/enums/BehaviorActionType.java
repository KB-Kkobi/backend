package org.kkobi.assessment.enums;

public enum BehaviorActionType {
    INITIAL_ALLOCATION,
    BUY,
    SELL,
    JOIN_PRODUCT,
    CANCEL_PRODUCT,
    MATURITY;

    public static BehaviorActionType getBehaviorActionType(String actionType) {
        return switch (actionType) {
            case "INITIAL_ALLOCATION" -> INITIAL_ALLOCATION;
            case "BUY" -> BUY;
            case "SELL" -> SELL;
            case "JOIN_PRODUCT", "JOIN_PRODUCTS", "SUBSCRIBE" -> JOIN_PRODUCT;
            case "CANCEL_PRODUCT", "CANCEL_PRODUCTS", "DEPOSIT_CANCEL", "TERMINATE" -> CANCEL_PRODUCT;
            case "MATURITY" -> MATURITY;
            default -> throw new IllegalArgumentException("지원하지 않는 행동 유형입니다: " + actionType);
        };
    }
}
