package org.kkobi.assessment.enums;

public enum VirtualInvestmentReferenceType {
    SECURITY_ORDER,
    PRODUCT_TRANSACTION;

    public static VirtualInvestmentReferenceType getReferenceType(String referenceType) {
        return switch (referenceType) {
            case "SECURITY_ORDER" -> SECURITY_ORDER;
            case "PRODUCT_TRANSACTION" -> PRODUCT_TRANSACTION;
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 원본 거래 유형입니다: " + referenceType
            );
        };
    }
}
