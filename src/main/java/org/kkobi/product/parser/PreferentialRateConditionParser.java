package org.kkobi.product.parser;

import org.kkobi.product.enums.PreferentialConditionType;
import org.kkobi.product.enums.PreferentialRateConditionRole;
import org.kkobi.product.parser.dto.ParsedPreferentialRateCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PreferentialRateConditionParser {

    // 6개월~1년처럼 월과 연도가 혼합된 기간 범위의 우대금리 추출
    private static final Pattern MONTH_YEAR_RANGE_RATE_PATTERN =
            Pattern.compile(
                    "(\\d+)\\s*개월\\s*[~～∼〜-]\\s*(\\d+)\\s*년(?:제)?"
                            + ".*?(\\d+(?:\\.\\d+)?)\\s*%\\s*p?"
            );

    // 문장에서 우대금리 숫자를 추출
    private static final Pattern RATE_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%\\s*p?");

    // 3~5개월 0.60%처럼 기간 범위별 우대금리를 추출
    private static final Pattern TERM_RANGE_RATE_PATTERN =
            Pattern.compile(
                    "(\\d+)\\s*[~～-]\\s*(\\d+)\\s*개월.*?(\\d+(?:\\.\\d+)?)\\s*%\\s*p?"
            );

    // 24개월 0.85%처럼 특정 가입기간의 우대금리를 추출
    private static final Pattern TERM_RATE_PATTERN =
            Pattern.compile(
                    "(\\d+)\\s*개월.*?(\\d+(?:\\.\\d+)?)\\s*%\\s*p?"
            );

    // 상품 옵션의 가입기간에 맞는 우대조건을 파싱
    public List<ParsedPreferentialRateCondition> parse(
            String preferentialConditions,
            Integer savingTerm
    ) {
        List<ParsedPreferentialRateCondition> result =
                new ArrayList<>();

        if (hasNoPreferentialCondition(preferentialConditions)) {
            return result;
        }

        // 같은 상품 옵션 안에서 사용할 그룹 번호
        long nextConditionGroupId = 1L;

        Long activeGroupId = null;
        GroupRule activeGroupRule = GroupRule.NONE;

        // 우대조건 원문을 줄 단위로 분리
        String[] lines =
                preferentialConditions.split("\\r?\\n");

        for (String line : lines) {
            String conditionText =
                    normalizeLine(line);

            if (conditionText.isEmpty()) {
                continue;
            }

            // 최고 우대금리 안내 문구는 개별 조건에서 제외
            if (isMaximumRateDescription(conditionText)) {
                continue;
            }

            BigDecimal additionalRate =
                    extractAdditionalRate(
                            conditionText,
                            savingTerm
                    );

            PreferentialConditionType conditionType =
                    classifyConditionType(
                            conditionText
                    );

            // 추가금리가 명확한 조건만 직접 선택 가능
            boolean selectable =
                    additionalRate != null;

            // 기본값은 독립 우대조건
            Long conditionGroupId = null;
            PreferentialRateConditionRole conditionRole =
                    PreferentialRateConditionRole.STANDALONE;

            // SC제일은행형 그룹 시작
            if (isAllRequiredBonusNotice(conditionText)) {
                activeGroupId = nextConditionGroupId++;
                activeGroupRule = GroupRule.ALL_REQUIRED_BONUS;

                conditionGroupId = activeGroupId;
                conditionRole =
                        PreferentialRateConditionRole.GROUP_NOTICE;
            }

            // 우리은행형 그룹 시작
            else if (isCommonPerformanceNotice(conditionText)) {
                activeGroupId = nextConditionGroupId++;
                activeGroupRule = GroupRule.COMMON_PERFORMANCE;

                conditionGroupId = activeGroupId;
                conditionRole =
                        PreferentialRateConditionRole.GROUP_NOTICE;
            }

            // SC제일은행형 그룹 내부 조건 처리
            else if (activeGroupRule == GroupRule.ALL_REQUIRED_BONUS) {
                conditionGroupId = activeGroupId;

                if (isBonusRateCondition(conditionText)) {
                    conditionRole =
                            PreferentialRateConditionRole.GROUP_CONDITION;

                    // 보너스 금리 조건 문장을 찾으면 그룹 종료
                    activeGroupId = null;
                    activeGroupRule = GroupRule.NONE;
                } else {
                    conditionRole =
                            PreferentialRateConditionRole.GROUP_DETAIL;
                }
            }

            // 우리은행형 그룹 내부 조건 처리
            else if (activeGroupRule == GroupRule.COMMON_PERFORMANCE) {
                if (isCommonPerformanceCondition(
                        conditionType,
                        additionalRate
                )) {
                    conditionGroupId = activeGroupId;
                    conditionRole =
                            PreferentialRateConditionRole.GROUP_CONDITION;
                } else {
                    // 확실하지 않은 조건은 억지로 그룹화하지 않음
                    activeGroupId = null;
                    activeGroupRule = GroupRule.NONE;
                }
            }

            result.add(
                    new ParsedPreferentialRateCondition(
                            conditionType,
                            conditionText,
                            additionalRate,
                            selectable,
                            conditionGroupId,
                            conditionRole
                    )
            );
        }

        return result;
    }

    // 모든 세부조건 충족이 필요한 보너스 우대 안내인지 확인
    private boolean isAllRequiredBonusNotice(
            String conditionText
    ) {
        String compact =
                conditionText.replaceAll("\\s+", "");

        boolean hasAllRequiredCondition =
                compact.contains("아래의조건을모두충족")
                        || compact.contains("아래조건을모두충족");

        return hasAllRequiredCondition
                && compact.contains("보너스이율");
    }

    // 실제 보너스 금리 조건 문장인지 확인
    private boolean isBonusRateCondition(
            String conditionText
    ) {
        String compact =
                conditionText.replaceAll("\\s+", "");

        return compact.contains("보너스이율")
                && RATE_PATTERN.matcher(conditionText).find();
    }

    // 여러 우대조건에 공통으로 적용되는 실적 안내인지 확인
    private boolean isCommonPerformanceNotice(
            String conditionText
    ) {
        String compact =
                conditionText.replaceAll("\\s+", "");

        return compact.contains("입출식계좌")
                && compact.contains("각항목별실적월수")
                && compact.contains("계약기간의1/2이상");
    }

    // 공통 실적 그룹에서 각각 금리가 적용되는 조건인지 확인
    private boolean isCommonPerformanceCondition(
            PreferentialConditionType conditionType,
            BigDecimal additionalRate
    ) {
        if (additionalRate == null) {
            return false;
        }

        return conditionType
                == PreferentialConditionType.INCOME_TRANSFER
                || conditionType
                == PreferentialConditionType.AUTOMATIC_TRANSFER
                || conditionType
                == PreferentialConditionType.CARD_USAGE;
    }

    // 줄 앞의 번호 및 불필요한 기호와 공백 제거
    private String normalizeLine(String line) {
        if (line == null) {
            return "";
        }

        return line.trim()
                .replaceFirst(
                        "^[①②③④⑤⑥⑦⑧⑨⑩\\d]+[.)]?\\s*",
                        ""
                )
                .replaceFirst(
                        "^[가나다라마바사아자차카타파하][.)]?\\s*",
                        ""
                )
                .replaceFirst(
                        "^[-▶*]+\\s*",
                        ""
                )
                .trim();
    }

    // 최고 우대금리 요약 문구인지 확인
    private boolean isMaximumRateDescription(
            String conditionText
    ) {
        String compact =
                conditionText.replaceAll("\\s+", "");

        return compact.startsWith("최고우대금리")
                || compact.startsWith("최고연")
                || compact.startsWith("우대이율(최대")
                || compact.startsWith("우대이율최대");
    }

    // 상품 가입기간에 맞는 추가 우대금리를 추출
    private BigDecimal extractAdditionalRate(
            String conditionText,
            Integer savingTerm
    ) {
        if (savingTerm == null) {
            return extractSingleRate(conditionText);
        }

        // 6개월~1년처럼 월과 연도가 혼합된 기간 범위 확인
        Matcher monthYearMatcher =
                MONTH_YEAR_RANGE_RATE_PATTERN.matcher(conditionText);

        while (monthYearMatcher.find()) {
            int startTerm =
                    Integer.parseInt(monthYearMatcher.group(1));

            int endTerm =
                    Integer.parseInt(monthYearMatcher.group(2)) * 12;

            if (savingTerm >= startTerm
                    && savingTerm <= endTerm) {
                return new BigDecimal(
                        monthYearMatcher.group(3)
                );
            }

            // 기간 범위에 해당하지 않으면 다른 패턴으로 잘못 해석하지 않도록 종료
            return null;
        }

        // 3~5개월처럼 기간 범위가 지정된 금리 확인
        Matcher rangeMatcher =
                TERM_RANGE_RATE_PATTERN.matcher(conditionText);

        while (rangeMatcher.find()) {
            int startTerm =
                    Integer.parseInt(rangeMatcher.group(1));

            int endTerm =
                    Integer.parseInt(rangeMatcher.group(2));

            if (savingTerm >= startTerm
                    && savingTerm <= endTerm) {
                return new BigDecimal(
                        rangeMatcher.group(3)
                );
            }

            return null;
        }

        // 24개월처럼 특정 가입기간에 지정된 금리 확인
        Matcher termMatcher =
                TERM_RATE_PATTERN.matcher(conditionText);

        while (termMatcher.find()) {
            int targetTerm =
                    Integer.parseInt(termMatcher.group(1));

            if (savingTerm == targetTerm) {
                return new BigDecimal(
                        termMatcher.group(2)
                );
            }
        }

        // 특정 기간 예외 부분을 제거한 뒤 일반 우대금리 확인
        String generalConditionText =
                removeTermSpecificDescriptions(conditionText);

        return extractSingleRate(generalConditionText);
    }

    // 기간별 예외 금리 문구를 제거
    private String removeTermSpecificDescriptions(
            String conditionText
    ) {
        if (conditionText == null) {
            return "";
        }

        // 괄호 안의 특정 기간 우대금리 문구 제거
        return conditionText.replaceAll(
                "\\([^)]*\\d+\\s*개월[^)]*\\)",
                ""
        );
    }

    // 금리가 하나만 명확하게 존재하는 경우 추출
    private BigDecimal extractSingleRate(
            String conditionText
    ) {
        Matcher matcher =
                RATE_PATTERN.matcher(conditionText);

        BigDecimal rate = null;
        int rateCount = 0;

        while (matcher.find()) {
            rateCount++;

            rate = new BigDecimal(
                    matcher.group(1)
            );
        }

        // 금리가 없거나 여러 개면 단일 적용금리를 결정할 수 없음
        if (rateCount != 1) {
            return null;
        }

        return rate;
    }

    // 조건 내용을 우대조건 유형으로 분류
    private PreferentialConditionType classifyConditionType(
            String conditionText
    ) {
        if (containsAny(
                conditionText,
                "급여",
                "연금",
                "소득이체"
        )) {
            return PreferentialConditionType.INCOME_TRANSFER;
        }

        if (containsAny(
                conditionText,
                "신용카드",
                "체크카드",
                "카드결제",
                "카드사용",
                "카드이용",
                "카드거래"
        )) {
            return PreferentialConditionType.CARD_USAGE;
        }

        if (containsAny(
                conditionText,
                "자동이체"
        )) {
            return PreferentialConditionType.AUTOMATIC_TRANSFER;
        }

        if (containsAny(
                conditionText,
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
        )) {
            return PreferentialConditionType.FIRST_TRANSACTION;
        }

        if (containsAny(
                conditionText,
                "주택청약",
                "청약보유",
                "청약 보유"
        )) {
            return PreferentialConditionType.HOUSING_SUBSCRIPTION;
        }

        if (containsAny(
                conditionText,
                "오픈뱅킹"
        )) {
            return PreferentialConditionType.OPEN_BANKING;
        }

        if (containsAny(
                conditionText,
                "비대면",
                "디지털 채널"
        )) {
            return PreferentialConditionType.NON_FACE_TO_FACE;
        }

        if (containsAny(
                conditionText,
                "마케팅",
                "혜택알림",
                "정보 수집",
                "정보수집"
        )) {
            return PreferentialConditionType.MARKETING_CONSENT;
        }

        return PreferentialConditionType.OTHER;
    }

    // 우대조건이 없는 상품인지 확인
    private boolean hasNoPreferentialCondition(
            String preferentialConditions
    ) {
        if (preferentialConditions == null
                || preferentialConditions.trim().isEmpty()) {
            return true;
        }

        String compact =
                preferentialConditions.replaceAll(
                        "\\s+",
                        ""
                );

        return compact.equals("없음")
                || compact.equals("해당없음")
                || compact.equals("해당무")
                || compact.equals("해당사항없음")
                || compact.equals("우대조건없음")
                || compact.equals("우대조건없이");
    }

    // 원문에 하나 이상의 키워드가 포함되어 있는지 확인
    private boolean containsAny(
            String text,
            String... keywords
    ) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String normalizedText =
                text.replaceAll("\\s+", "");

        for (String keyword : keywords) {
            String normalizedKeyword =
                    keyword.replaceAll("\\s+", "");

            if (normalizedText.contains(
                    normalizedKeyword
            )) {
                return true;
            }
        }

        return false;
    }

    // 현재 적용 중인 우대조건 그룹 규칙
    private enum GroupRule {
        NONE,
        ALL_REQUIRED_BONUS,
        COMMON_PERFORMANCE
    }
}