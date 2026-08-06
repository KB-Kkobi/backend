package org.kkobi.assessment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssessmentPeriodType {
    DAILY_ASSESSMENT_BATCH(false),
    SEVEN_DAY_ALLOCATION(true),
    WEEKLY_TRADE_FREQUENCY(false),
    WEEKLY_CASH_RATIO(false),
    LONG_SECURITY_HOLDING(true);

    private final boolean permanent;
}
