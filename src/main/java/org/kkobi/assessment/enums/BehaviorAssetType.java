package org.kkobi.assessment.enums;

public enum BehaviorAssetType {
    ALL,
    SECURITY,
    PRODUCT,
    CASH;

    public static BehaviorAssetType getBehaviorAssetType(String assetType) {
        return switch (assetType) {
            case "ALL" -> ALL;
            case "STOCK", "SECURITY" -> SECURITY;
            case "DEPOSIT", "PRODUCT", "PRODUCTS" -> PRODUCT;
            case "CASH" -> CASH;
            default -> throw new IllegalArgumentException("지원하지 않는 자산 유형입니다: " + assetType);
        };
    }
}
