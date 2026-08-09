package org.kkobi.product.parser;

import org.kkobi.product.enums.PreferentialConditionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Component
public class PreferentialConditionParser {

    // 우대조건 원문에서 필터링 가능한 우대조건 유형을 추출
    public List<PreferentialConditionType> parse(String preferentialConditions) {

        if(hasNoPreferenticalCondition(preferentialConditions)) {
            return new ArrayList<>();
        }

        EnumSet<PreferentialConditionType> conditionTypes =
                EnumSet.noneOf(PreferentialConditionType.class);

        // 급여, 연급, 소득이체 조건 확인
        if(containsAny(
                preferentialConditions,
                "급여",
                "연금",
                "소득이체"
        )) {
            conditionTypes.add(PreferentialConditionType.INCOME_TRANSFER);
        }

        // 신용, 체크카드 이용 조건 확인
        if(containsAny(
                preferentialConditions,
                "신용카드",
                "체크카드",
                "카드결제",
                "카드사용",
                "카드 사용",
                "카드이용",
                "카드 이용",
                "카드 거래"
        )) {
            conditionTypes.add(PreferentialConditionType.CARD_USAGE);
        }

        // 자동이체 조건 확인
        if(containsAny(
                preferentialConditions,
                "자동이체"
        )) {
            conditionTypes.add(PreferentialConditionType.AUTOMATIC_TRANSFER);
        }

        // 첫 거래, 신규 고객 조건 확인
        if(containsAny(
                preferentialConditions,
                "첫거래",
                "첫 거래",
                "최초거래",
                "최초 거래",
                "최초신규",
                "최초 신규",
                "신규고객",
                "신규 고객",
                "첫예금거래",
                "첫예금 거래"
        )){
            conditionTypes.add(PreferentialConditionType.MARKETING_CONSENT);
        }

        // 주택청약 보유 조건 확인
        if(containsAny(
                preferentialConditions,
                "주택청약",
                "청약보유",
                "청약 보유"
        )) {
            conditionTypes.add(PreferentialConditionType.HOUSING_SUBSCRIPTION);
        }

        // 오픈뱅킹 이용 조건 확인
        if(containsAny(
                preferentialConditions,
                "오픈뱅킹"
        )) {
            conditionTypes.add(PreferentialConditionType.OPEN_BANKING);
        }

        // 비대면 채널 이용 조건 확인
        if(containsAny(
                preferentialConditions,
                "비대면",
                "디지털 채널"
        )) {
            conditionTypes.add(PreferentialConditionType.NON_FACE_TO_FACE);
        }

        // 분류되지 않은 우대조건은 기타로 처리
        if (conditionTypes.isEmpty()) {
            conditionTypes.add(PreferentialConditionType.OTHER);
        }

        return new ArrayList<>(conditionTypes);
    }

    // 우대조건이 없느 상품인지 확인
    private boolean hasNoPreferenticalCondition(String preferentialConditions) {

        if(preferentialConditions == null || preferentialConditions.trim().isEmpty()) {
            return true;
        }

        String compactConditions =
                preferentialConditions.replaceAll("\\s+","");

        return compactConditions.equals("없음")
                || compactConditions.equals("해당없음")
                || compactConditions.equals("해당무")
                || compactConditions.equals("해당사항없음")
                || compactConditions.equals("우대조건없음")
                || compactConditions.equals("우대조건없이");
    }

    // 원문에 하나 이상의 키워드가 포함되어 있는지 확인
    private boolean containsAny(String text, String... keywords){

        for(String keyword : keywords){
            if(text.contains(keyword)){
                return true;
            }
        }

        return false;
    }
}
