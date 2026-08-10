package org.kkobi.product.enums;

public enum PreferentialConditionType {

    // 급여, 연금, 소득이체 조건
    INCOME_TRANSFER,

    // 신용, 체크카드 이용 조건
    CARD_USAGE,

    // 자동이체 조건
    AUTOMATIC_TRANSFER,

    // 첫 거래, 신규 고객 조건
    FIRST_TRANSACTION,

    // 마케팅, 개인정보 활용 동의 조건
    MARKETING_CONSENT,

    // 주택청약 보유 조건
    HOUSING_SUBSCRIPTION,

    // 오픈뱅킹 이용 조건
    OPEN_BANKING,

    // 비대면 채널 가입 조건
    NON_FACE_TO_FACE,

    // 기타 우대조건
    OTHER
}
