package org.kkobi.securities.enums;

public enum StockSortType {
    MATCH, CHANGE, VOLUME, NAME;

    // 소문자 문자열("match" 등) → enum 변환. 허용값 외 입력은 MATCH(기본값)로 처리
    public static StockSortType fromString(String value) {
        if (value == null || value.isBlank()) {
            return MATCH;
        }
        try {
            return StockSortType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MATCH;
        }
    }
}
